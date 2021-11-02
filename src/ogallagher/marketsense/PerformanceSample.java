package ogallagher.marketsense;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import ogallagher.temp_fx_logger.System;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.TrainingSession;
import ogallagher.marketsense.persistent.TrainingSessionId;
import ogallagher.marketsense.util.ConfidenceZscore;

/**
 * Encapsulates performance data, fetched from the marketsense database, given a set of filters.
 * 
 * @author Owen Gallagher
 * @since 2021-11-02
 *
 */
public class PerformanceSample {
	private Person person;
	private Security security;
	private LocalDate startDate;
	private LocalDate endDate;
	private String barWidth;
	private int sampleSize;
	private int sampleCount;
	
	private ArrayList<PerformancePoint> points;
	
	/**
	 * Performance sample constructor.
	 * 
	 * @param person
	 * @param security
	 * @param startDate
	 * @param endDate
	 * @param barWidth
	 * @param sampleSize
	 * @param sampleCount
	 */
	public PerformanceSample(
		Person person, Security security, 
		LocalDate startDate, LocalDate endDate,
		String barWidth, int sampleSize, int sampleCount) {
		this.person = person;
		this.security = security;
		this.startDate = startDate;
		this.endDate = endDate;
		this.barWidth = barWidth;
		this.sampleSize = sampleSize;
		this.sampleCount = sampleCount;
		
		points = new ArrayList<>();
	}
	
	private Query createQuery(EntityManager dbManager) {
		String qstr = String.format(
			"select s from %1$s s",
			TrainingSession.DB_TABLE
		);
		LinkedList<String> wheres = new LinkedList<>();
		
		if (person != null) {
			wheres.add(String.format(
				"s.%1$s.%2$s = :person",
				TrainingSession.DB_COL_ID,
				TrainingSessionId.DB_COL_PERSON
			));
		}
		if (security != null) {
			wheres.add(String.format(
				"s.%1$s = :securitySymbol and s.%2$s = :securityExchange", 
				TrainingSession.DB_COL_SEC_SYMBOL,
				TrainingSession.DB_COL_SEC_EXCHANGE
			));
		}
		if (startDate != null) {
			wheres.add(String.format(
				"s.%1$s.%2$s >= :startDate", 
				TrainingSession.DB_COL_ID,
				TrainingSessionId.DB_COL_START
			));
		}
		if (endDate != null) {
			wheres.add(String.format(
				"s.%1$s.%2$s <= :endDate", 
				TrainingSession.DB_COL_ID,
				TrainingSessionId.DB_COL_START
			));
		}
		if (barWidth != null) {
			wheres.add(String.format(
				"s.%1$s = :barWidth",
				TrainingSession.DB_COL_BAR_WIDTH
			));
		}
		if (sampleSize != -1) {
			wheres.add(String.format(
				"s.%1$s = :sampleSize",
				TrainingSession.DB_COL_SAMPLE_SIZE
			));
		}
		if (sampleCount != -1) {
			wheres.add(String.format(
				"s.%1$s = :sampleCount",
				TrainingSession.DB_COL_SAMPLE_COUNT
			));
		}
		/*
		if (maxLookbackMonths != null) {
			wheres.add(String.format(
				"",
				TrainingSession.DB_COL_
			));
		}
		*/
		
		// close where clause
		if (!wheres.isEmpty()) {
			qstr += " where " + String.join(" and ", wheres);
		}
		
		qstr += String.format(
			" order by s.%1$s.%2$s asc", 
			TrainingSession.DB_COL_ID,
			TrainingSessionId.DB_COL_START
		);
		
		Query query = dbManager.createQuery(qstr);
		System.out.println("debug will execute db query:\n" + qstr);
		
		if (person != null) {
			query.setParameter("person", person);
		}
		if (security != null) {
			query.setParameter("securitySymbol", security.getSymbol());
			query.setParameter("securityExchange", security.getExchange());
		}
		if (startDate != null) {
			query.setParameter("startDate", startDate);
		}
		if (endDate != null) {
			query.setParameter("endDate", endDate);
		}
		if (barWidth != null) {
			query.setParameter("barWidth", barWidth);
		}
		if (sampleSize != -1) {
			query.setParameter("sampleSize", sampleSize);
		}
		if (sampleCount != -1) {
			query.setParameter("sampleCount", sampleCount);
		}
		/*
		if (maxLookbackMonths != null) {
			query.setParameter("maxLookbackMonths", maxLookbackMonths);
		}
		*/
		
		return query;
	}
	
	/**
	 * Fetch required data from the database.
	 */
	@SuppressWarnings("unchecked")
	public void prepare(EntityManager dbManager) {
		Query query = createQuery(dbManager);
		
		// update performance points
		points.clear();
		
		for (TrainingSession session : (List<TrainingSession>) query.getResultList()) {
			points.add(new PerformancePoint(session));
		}
	}
	
	public List<PerformancePoint> getPoints() {
		return points;
	}
	
	public String getTitle() {
		return person.getUsername() + " " + security.getSymbol();
	}
	
	public static class PerformancePoint {
		private TrainingSession session;
		
		public PerformancePoint(TrainingSession session) {
			this.session = session;
		}
		
		/**
		 * <p>{@code radius = confidence.getZscore() * deviation}</p>
		 * 
		 * @return Score confidence interval radius, as in: {@code interval = mean +- radius}.
		 */
		public double getScoreIntervalRadius(ConfidenceZscore confidence) {
			return confidence.getZscore() * getScoreDeviation();
		}
		
		public double getScore() {
			return session.getScoreProperty().get();
		}
		
		public double getScoreDeviation() {
			return session.getScoreDeviationProperty().get();
		}
		
		public double getScoreIntervalLow(ConfidenceZscore confidence) {
			return getScore() - getScoreIntervalRadius(confidence);
		}
		
		public double getScoreIntervalHigh(ConfidenceZscore confidence) {
			return getScore() + getScoreIntervalRadius(confidence);
		}
	}
}
