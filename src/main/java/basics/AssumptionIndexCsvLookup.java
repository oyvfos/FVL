package basics;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard Assumption Index implementations from CSV.
 * <p>
 * See {@link AssumptionIndices} for the description of each.
 */
final class AssumptionIndexCsvLookup
    implements NamedLookup<AssumptionIndex> {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(AssumptionIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final AssumptionIndexCsvLookup INSTANCE = new AssumptionIndexCsvLookup();

  // CSV column headers
  private static final String NAME_FIELD = "Name";
  private static final String CURRENCY_FIELD = "Currency";
  private static final String COUNTRY_FIELD = "Country";
  private static final String ACTIVE_FIELD = "Active";
  private static final String PUBLICATION_FREQUENCY_FIELD = "Publication Frequency";

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, AssumptionIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private AssumptionIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, AssumptionIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, AssumptionIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("AssumptionIndexData.csv");
    Map<String, AssumptionIndex> map = new HashMap<>();
    // files are ordered lowest priority to highest, thus Map::put is used
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (CsvRow row : csv.rows()) {
          AssumptionIndex parsed = parseAssumptionIndex(row);
          map.put(parsed.getName(), parsed);
          map.put(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Assumption Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static AssumptionIndex parseAssumptionIndex(CsvRow row) {
    String name = row.getField(NAME_FIELD);
    Currency currency = Currency.parse(row.getField(CURRENCY_FIELD));
    Country region = Country.of(row.getField(COUNTRY_FIELD));
    boolean active = Boolean.parseBoolean(row.getField(ACTIVE_FIELD));
    Frequency frequency = Frequency.parse(row.getField(PUBLICATION_FREQUENCY_FIELD));
    // build result
    return ImmutableAssumptionIndex.builder()
        .name(name)
        .currency(currency)
        .region(region)
        .active(active)
        .publicationFrequency(frequency)
        .build();
  }

}
