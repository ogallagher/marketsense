package ogallagher.marketsense.test;

import java.util.List;

import javax.persistence.EntityManager;

import ogallagher.marketsense.MarketSense;
import ogallagher.marketsense.persistent.Person;
import ogallagher.temp_fx_logger.System;

public class TestDatabase extends Test {
	static {
		TestDatabase.name = "db";
	}
	
	public static void dummyPeople(EntityManager dbManager) {
		String[] names = {
				"aaron","bethany","charlie","dennis","emma","flapjack","giovanni","henry"
		};
		System.out.println("inserting " + names.length + " dummy people");
		
		dbManager.getTransaction().begin();
		for (String name : names) {
			Person p = new Person(name);
			System.out.println("persist " + p);
			dbManager.persist(p);
		}
		dbManager.getTransaction().commit();
		
		@SuppressWarnings("unchecked")
		List<Person> people = (List<Person>) dbManager
		.createQuery("select p from " + Person.DB_TABLE + " p")
		.getResultList();
		System.out.println("loaded " + people.size() + " people from db");
	}
	
	@Override
	public void evaluate() {
		System.out.println("testing " + name);
		dummyPeople(MarketSense.dbManager);
		System.out.println(name + " testing complete");
	}
}
