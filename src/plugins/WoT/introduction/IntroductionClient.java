/*
 * This code is part of WoT, a plugin for Freenet. It is distributed 
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL.
 */
package plugins.WoT.introduction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import plugins.WoT.Identity;
import plugins.WoT.OwnIdentity;
import plugins.WoT.exceptions.NotInTrustTreeException;
import plugins.WoT.introduction.IntroductionPuzzle.PuzzleType;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;

import freenet.client.ClientMetadata;
import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutter;
import freenet.keys.FreenetURI;
import freenet.node.RequestStarter;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.io.TempBucketFactory;

/**
 * This class allows the user to announce new identities:
 * It downloads puzzles from known identites and uploads solutions of the puzzles.
 * 
 * @author xor
 *
 */
public final class IntroductionClient implements Runnable, ClientCallback  {
	
	private static final long STARTUP_DELAY = 1 * 60 * 1000;
	private static final long THREAD_PERIOD = 10 * 60 * 1000; /* FIXME: tweak before release: */ 
	
	public static final byte PUZZLE_DOWNLOAD_BACKWARDS_DAYS = IntroductionServer.PUZZLE_INVALID_AFTER_DAYS - 1;
	public static final int PUZZLE_REQUEST_COUNT = 16;
	public static final int PUZZLE_POOL_SIZE = 128;
	
	/* FIXME: Display a random puzzle of an identity in the UI instead of always the first one! Otherwise we should really have each identity only
	 * insert one puzzle per day. But it might be a good idea to allow many puzzles per identity: Our seed identity could be configured to
	 * insert a very large amount of puzzles and therefore help the WoT while it is small */
	public static final int MAX_PUZZLES_PER_IDENTITY = IntroductionServer.PUZZLE_COUNT;

	private static final int MINIMUM_SCORE_FOR_PUZZLE_DOWNLOAD = 10; /* FIXME: tweak before release */
	
	private static final int MINIMUM_SCORE_FOR_PUZZLE_DISPLAY = 50; /* FIXME: tweak before release */

	private Thread mThread;
	
	/** Used to tell the introduction server thread if it should stop */
	private volatile boolean isRunning;
	
	/** A reference to the database */
	private ObjectContainer db;

	/** A reference the HighLevelSimpleClient used to perform inserts */
	private HighLevelSimpleClient mClient;
	
	/** The TempBucketFactory used to create buckets from puzzles before insert */
	private final TempBucketFactory mTBF;
	
	/** All current requests */
	/* FIXME FIXME FIXME: Use LRUQueue instead. ArrayBlockingQueue does not use a Hashset for contains()! */
	private final ArrayBlockingQueue<Identity> mIdentities = new ArrayBlockingQueue<Identity>(PUZZLE_POOL_SIZE); /* FIXME: figure out whether my assumption that this is just the right size is correct */
	private final HashSet<ClientGetter> mRequests = new HashSet<ClientGetter>(PUZZLE_REQUEST_COUNT * 2); /* TODO: profile & tweak */
	
	private final HashSet<ClientPutter> mInserts = new HashSet<ClientPutter>(PUZZLE_REQUEST_COUNT * 2); 

	/**
	 * Creates an IntroductionServer
	 * 
	 * @param db
	 *            A reference to the database
	 * @param client
	 *            A reference to an {@link HighLevelSimpleClient} to perform
	 *            inserts
	 * @param tbf
	 *            Needed to create buckets from Identities before insert
	 */
	public IntroductionClient(ObjectContainer myDB, HighLevelSimpleClient myClient, TempBucketFactory myTBF) {
		isRunning = true;
		db = myDB;
		mClient = myClient;
		mTBF = myTBF;
	}

	public void run() {
		Logger.debug(this, "Introduction client thread started.");
		
		mThread = Thread.currentThread();
		try {
			Thread.sleep((long) (1*60*1000 * (0.5f + Math.random()))); // Let the node start up
		}
		catch (InterruptedException e)
		{
			mThread.interrupt();
		}
		
		while(isRunning) {
			Logger.debug(this, "Introduction client loop running...");
			
			IntroductionPuzzle.deleteExpiredPuzzles(db);
			downloadPuzzles();
			
			Logger.debug(this, "Introduction client loop finished.");
			
			try {
				Thread.sleep((long) (THREAD_PERIOD * (0.5f + Math.random())));
			}
			catch (InterruptedException e)
			{
				mThread.interrupt();
				Logger.debug(this, "Introduction client loop interrupted.");
			}
		}
		
		cancelRequests();
		Logger.debug(this, "Introduction client thread finished.");
	}
	
	public void terminate() {
		Logger.debug(this, "Stopping the introduction client...");
		isRunning = false;
		mThread.interrupt();
		try {
			mThread.join();
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		Logger.debug(this, "Stopped the introduction client.");
	}
	
	public synchronized List<IntroductionPuzzle> getPuzzles(PuzzleType puzzleType, OwnIdentity id, int count) {
		Query q = db.query();
		q.constrain(IntroductionPuzzle.class);
		q.descend("mType").constrain(puzzleType);
		q.descend("mSolution").constrain(null).identity(); /* FIXME: toad said constrain(null) is maybe broken. If this is true: Alternative would be: q.descend("mIdentity").constrain(OwnIdentity.class).not(); */
		ObjectSet<IntroductionPuzzle> puzzles = q.execute();
		ArrayList<IntroductionPuzzle> result = new ArrayList<IntroductionPuzzle>(count);
		HashSet<Identity> resultHasPuzzleFrom = new HashSet<Identity>(count);
		
		for(IntroductionPuzzle p : puzzles) {
			try {
				int score = p.getInserter().getScore(id, db).getScore();
				if(score > MINIMUM_SCORE_FOR_PUZZLE_DISPLAY && !resultHasPuzzleFrom.contains(p.getInserter())) { 
					result.add(p);
					resultHasPuzzleFrom.add(p.getInserter());
					if(result.size() == count)
						break;
				}
			}
			catch(NotInTrustTreeException e) {
			}
		}
		
		return result;
	}
	
	public synchronized void insertPuzzleSolution(IntroductionPuzzle p, String solution, OwnIdentity solver) throws IOException, ParserConfigurationException, TransformerException, InsertException {
		Bucket tempB = mTBF.makeBucket(10 * 1024); /* TODO: set to a reasonable value */
		OutputStream os = tempB.getOutputStream();

		try {
			solver.exportIntroductionToXML(os);
			os.close(); os = null;
			tempB.setReadOnly();

			ClientMetadata cmd = new ClientMetadata("text/xml");
			FreenetURI solutionURI = p.getSolutionURI(solution);
			InsertBlock ib = new InsertBlock(tempB, cmd, solutionURI);

			InsertContext ictx = mClient.getInsertContext(true);
			
			/* FIXME: are these parameters correct? */
			ClientPutter pu = mClient.insert(ib, false, null, false, ictx, this);
			pu.setPriorityClass(RequestStarter.UPDATE_PRIORITY_CLASS);
			mInserts.add(pu);
			tempB = null;
			
			Logger.debug(this, "Started to insert puzzle solution of " + solver.getNickName() + " at " + solutionURI);

			p.setSolved(solver, solution);
			db.store(p);
			db.commit();
		}
		finally {
			if(tempB != null)
				tempB.free();
			if(os != null)
				os.close();
		}
	}
	
	private synchronized void cancelRequests() {
		Iterator<ClientGetter> r = mRequests.iterator();
		Iterator<ClientPutter> i = mInserts.iterator();
		int rcounter = 0;
		int icounter = 0;
		Logger.debug(this, "Trying to stop all requests & inserts"); 
		while (r.hasNext()) { r.next().cancel(); ++rcounter; }
		while (i.hasNext()) { i.next().cancel(); ++icounter; }
		Logger.debug(this, "Stopped " + rcounter + " current requests");
		Logger.debug(this, "Stopped " + icounter + " current inserts");
	}
	
	private synchronized void downloadPuzzles() {
		Query q = db.query();
		q.constrain(Identity.class);
		q.constrain(OwnIdentity.class).not();
		q.descend("lastChange").constrain(new Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000)).greater();
		q.descend("lastChange").orderDescending(); /* This should choose identities in a sufficiently random order */
		ObjectSet<Identity> allIds = q.execute();
		ArrayList<Identity> ids = new ArrayList<Identity>(PUZZLE_POOL_SIZE);
		
		int counter = 0;
		for(Identity i : allIds) {
			/* TODO: Create a "boolean providesIntroduction" in Identity to use a database query instead of this */ 
			if(i.hasContext(IntroductionPuzzle.INTRODUCTION_CONTEXT) && !mIdentities.contains(i)
					&& i.getBestScore(db) > MINIMUM_SCORE_FOR_PUZZLE_DOWNLOAD)  {
				ids.add(i);
				++counter;
			}

			if(counter == PUZZLE_REQUEST_COUNT)
				break;
		}
		
		if(counter == 0) {
			ids.clear();  /* We probably have less updated identities today than the size of the LRUQueue, empty it */

			for(Identity i : allIds) {
				/* TODO: Create a "boolean providesIntroduction" in Identity to use a database query instead of this */ 
				if(i.hasContext(IntroductionPuzzle.INTRODUCTION_CONTEXT) && i.getBestScore(db) > MINIMUM_SCORE_FOR_PUZZLE_DOWNLOAD)  {
					ids.add(i);
					++counter;
				}

				if(counter == PUZZLE_REQUEST_COUNT)
					break;
			}
		}

		
		/* I suppose its a good idea to restart downloading the puzzles from the latest updated identities every time the thread iterates
		 * This prevents denial of service because people will usually get very new puzzles. */
		cancelRequests();
		
		for(Identity i : ids) {
			try {
				downloadPuzzle(i, 0);
			} catch (Exception e) {
				Logger.error(this, "Starting puzzle download failed.", e);
			}
		}
		
	}
		
	private synchronized void downloadPuzzle(Identity identity, int index) throws FetchException {
		FreenetURI uri = IntroductionPuzzle.generateRequestURI(identity, new Date(), index);
		
		FetchContext fetchContext = mClient.getFetchContext();
		fetchContext.maxSplitfileBlockRetries = -1; // retry forever
		fetchContext.maxNonSplitfileRetries = -1; // retry forever
		ClientGetter g = mClient.fetch(uri, -1, this, this, fetchContext);
		g.setPriorityClass(RequestStarter.UPDATE_PRIORITY_CLASS); /* FIXME: decide which one to use */
		mRequests.add(g);
		if(!mIdentities.contains(identity)) {
			mIdentities.poll();
			try {
			mIdentities.put(identity);
			} catch(InterruptedException e) {}
		}
		Logger.debug(this, "Trying to fetch puzzle from " + uri.toString());
	}


	/**
	 * Called when the node can't fetch a file OR when there is a newer edition.
	 * In our case, called when there is no puzzle available.
	 */
	public synchronized void onFailure(FetchException e, ClientGetter state) {
		Logger.normal(this, "Downloading puzzle " + state.getURI() + " failed.", e);

		mRequests.remove(state);
	}

	/**
	 * Called when a puzzle is successfully fetched.
	 */
	public synchronized void onSuccess(FetchResult result, ClientGetter state) {
		Logger.debug(this, "Fetched puzzle: " + state.getURI());

		try {
			IntroductionPuzzle p = IntroductionPuzzle.importFromXML(db, result.asBucket().getInputStream(), state.getURI());
			IntroductionPuzzle.deleteOldestPuzzles(db, PUZZLE_POOL_SIZE);
			state.cancel(); /* FIXME: is this necessary */ 
			mRequests.remove(state);
			if(p.getIndex() < MAX_PUZZLES_PER_IDENTITY) {
				downloadPuzzle(p.getInserter(), p.getIndex() + 1);
			}
		} catch (Exception e) { 
			Logger.error(this, "Parsing failed for "+ state.getURI(), e);
		}
	}

	// Only called by inserts
	public synchronized void onSuccess(BaseClientPutter state)
	{
		Logger.debug(this, "Successful insert of puzzle solution at " + state.getURI());
		state.cancel(); /* FIXME: is this necessary */
		mInserts.remove(state);
	}
	
	// Only called by inserts
	public synchronized void onFailure(InsertException e, BaseClientPutter state)
	{
		Logger.debug(this, "Insert of puzzle solution failed for " + state.getURI(), e);
		state.cancel(); /* FIXME: is this necessary */
		mInserts.remove(state);
	}
	
	/* Not needed functions from the ClientCallback inteface */

	// Only called by inserts
	public void onFetchable(BaseClientPutter state) {}

	// Only called by inserts
	public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {}

	/** Called when freenet.async thinks that the request should be serialized to
	 * disk, if it is a persistent request. */
	public void onMajorProgress() {}
}