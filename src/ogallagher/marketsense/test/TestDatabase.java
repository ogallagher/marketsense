package ogallagher.marketsense.test;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.SecurityId;
import ogallagher.marketsense.persistent.SecurityType;
import ogallagher.marketsense.persistent.TradeBar;
import ogallagher.marketsense.persistent.TrainingSession;
import ogallagher.marketsense.persistent.TrainingSessionType;
import ogallagher.temp_fx_logger.System;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;

public class TestDatabase extends Test {
	private EntityManager dbManager;
	
	static {
		TestDatabase.name = "db";
	}
	
	public TestDatabase(EntityManager dbManager) {
		this.dbManager = dbManager;
	}
	
	@SuppressWarnings("unchecked")
	public void dummyPeople(boolean persistDummies, boolean testTrainingSessions) {
		String[] names = {
			"aaron","bethany","charlie","dennis","emma","flapjack","giovanni","henry"
		};
		Person[] dummyPeople = new Person[names.length];
		System.out.println("testing " + dummyPeople.length + " dummy people");
		
		// find or insert dummy people
		
		dbManager.getTransaction().begin();
		int n=0;
		for (String name : names) {
			Query q = dbManager.createQuery(
				"select p from " + Person.DB_TABLE + " p where p." + Person.DB_COL_USERNAME + " = :username"
			).setParameter("username", name);
			
			List<Person> dbp = (List<Person>) q.getResultList();
			Person p;
			
			if (dbp.isEmpty()) {
				p = new Person(name);
				System.out.println("persist new dummy person " + p);
				dbManager.persist(p);
			}
			else {
				p = dbp.get(0);
				System.out.println("found dummy person " + p);
			}
			
			dummyPeople[n++] = p;
		}
		dbManager.getTransaction().commit();
		
		if (testTrainingSessions) {
			dummyTrainingSessions(persistDummies, dummyPeople);
		}
		
		if (!persistDummies) {
			// delete dummy people
			
			System.out.println("removing dummy people");
			dbManager.getTransaction().begin();
			for (Person p : dummyPeople) {
				// remove dummy
				dbManager.remove(p);
			}
			dbManager.getTransaction().commit();
		}
	}
	
	private void dummyTrainingSessions(boolean persistDummies, Person[] people) {
		int sessionsPerPerson = 3;
		TrainingSession[] dummySessions = new TrainingSession[sessionsPerPerson * people.length];
		int i=0;
		for (int p=0; p<people.length; p++) {
			for (int s=0; s<sessionsPerPerson; s++) {
				dummySessions[i] = new TrainingSession(
					people[p],
					TrainingSessionType.TBD, 
					new Security(), 
					BarInterval.DY_1, 
					10, 
					15, 
					2
				);
				i++;
			}
		}
		System.out.println("testing " + dummySessions.length + " dummy training sessions");
		
		// find or insert dummy sessions
		dbManager.getTransaction().begin();
		for (int s=0; s<dummySessions.length; s++) {
			TrainingSession ds = dummySessions[s];
			
			if (dbManager.contains(ds)) {
				dbManager.refresh(ds);
				System.out.println("found dummy training session " + ds);
			}
			else {
				System.out.println("persist new dummy training session " + ds);
				dbManager.persist(ds);
			}
		}
		dbManager.getTransaction().commit();
		
		if (!persistDummies) {
			// delete dummy sessions
			System.out.println("deleting dummy training sessions");
			
			dbManager.getTransaction().begin();
			for (TrainingSession ds : dummySessions) {
				dbManager.remove(ds);
			}
			dbManager.getTransaction().commit();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void dummySecurities(boolean persistDummies, boolean testTradeBars) {
		Security[] dummySecurities = new Security[] {
			new Security("AAA","XXX",SecurityType.STOCK),
			new Security("BBB","YYY",SecurityType.ETF),
			new Security("CCC","ZZZ",SecurityType.FOREX),
			new Security("DDD","ZZZ",SecurityType.FUTURE)
		};
		System.out.println("testing " + dummySecurities.length + " dummy securities");
		
		// find or insert dummy securities
		
		dbManager.getTransaction().begin();
		for (int i=0; i<dummySecurities.length; i++) {
			Security ds = dummySecurities[i];
			
			Query q = dbManager.createQuery(
				"select s from " + Security.DB_TABLE + " s where s." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL + " = :symbol " +
				"and s." + Security.DB_COL_ID + "." + SecurityId.DB_COL_EXCHANGE + " = :exchange"
			);
			q.setParameter("symbol", ds.getSymbol());
			q.setParameter("exchange", ds.getExchange());
			
			List<Security> dbs = (List<Security>) q.getResultList();
			if (dbs.isEmpty()) {
				System.out.println("persist new dummy security " + ds);
				dbManager.persist(ds);
			}
			else {
				ds = dbs.get(0);
				dummySecurities[i] = ds;
				System.out.println("found dummy security " + ds);
			}
		}
		dbManager.getTransaction().commit();
		
		if (testTradeBars) {
			dummyTradeBars(persistDummies, dummySecurities);
		}
		
		if (!persistDummies) {
			// delete dummy securities
			
			dbManager.getTransaction().begin();
			System.out.println("removing dummy securities");
			for (Security ds : dummySecurities) {
				dbManager.remove(ds);
			}
			dbManager.getTransaction().commit();
		}
	}
	
	private void dummyTradeBars(boolean persistDummies, Security[] securities) {
		int barsPerSecurity = 10;
		TradeBar[] dummyBars = new TradeBar[barsPerSecurity * securities.length];
		int i=0;
		for (int s=0; s<securities.length; s++) {
			for (int b=0; b<barsPerSecurity; b++) {
				dummyBars[i] = new TradeBar(securities[s],LocalDateTime.now().minusMinutes(b),BarInterval.DY_1,i,i,i,i);
				i++;
			}
		}
		System.out.println("testing " + dummyBars.length + " dummy trade bars");
		
		dbManager.getTransaction().begin();
		for (int t=0; t<dummyBars.length; t++) {
			TradeBar db = dummyBars[t];
			
			if (dbManager.contains(db)) {
				dbManager.refresh(db);
				System.out.println("found dummy trade bar " + db);
			}
			else {
				System.out.println("persist new dummy trade bar " + db);
				dbManager.persist(db);
			}
		}
		dbManager.getTransaction().commit();
		
		if (!persistDummies) {
			// delete dummy trade bars
			System.out.println("deleting dummy trade bars");
			
			dbManager.getTransaction().begin();
			for (TradeBar db : dummyBars) {
				dbManager.remove(db);
			}
			dbManager.getTransaction().commit();
		}
	}
	
	@Override
	public void evaluate(boolean persistDummies) {
		System.out.println("testing " + name);
		
		dummyPeople(persistDummies,true);
		
//		dummySecurities(persistDummies,true);
		
		System.out.println(name + " testing complete");
	}
}
