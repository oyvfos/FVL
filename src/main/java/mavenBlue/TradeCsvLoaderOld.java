package mavenBlue;

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CPTY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CPTY_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SETTLEMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TIME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_ZONE_FIELD;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.UnicodeBom;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.TradeCsvInfoResolver;
import com.opengamma.strata.loader.csv.TradeCsvParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradeInfoBuilder;

public final class TradeCsvLoaderOld {

	  // default schemes
	  private static final String DEFAULT_TRADE_SCHEME = StandardSchemes.OG_TRADE_SCHEME;
	  private static final String DEFAULT_CPTY_SCHEME = StandardSchemes.OG_COUNTERPARTY;

	  /**
	   * The lookup of trade parsers.
	   */
	  static final ExtendedEnum<TradeCsvParserPlugin> ENUM_LOOKUP = ExtendedEnum.of(TradeCsvParserPlugin.class);
	  /**
	   * The lookup of trade parsers.
	   */
	  private static final ImmutableMap<String, TradeCsvParserPlugin> PLUGINS =
	      MapStream.of(TradeCsvParserPlugin.extendedEnum().lookupAllNormalized().values())
	          .flatMapKeys(plugin -> plugin.tradeTypeNames().stream())
	          .toMap((a, b) -> {
	            System.err.println("Two plugins declare the same product type: " + a.tradeTypeNames());
	            return a;
	          });

	  /**
	   * The resolver, providing additional information.
	   */
	  private final TradeCsvInfoResolverPolicy resolver;

	  //-------------------------------------------------------------------------
	  /**
	   * Obtains an instance that uses the standard set of reference data.
	   * 
	   * @return the loader
	   */
	  public static TradeCsvLoaderOld standard() {
	    return new TradeCsvLoaderOld(TradeCsvInfoResolverPolicy.standard());
	  }

	  /**
	   * Obtains an instance that uses the specified set of reference data.
	   * 
	   * @param refData  the reference data
	   * @return the loader
	   */
	  public static TradeCsvLoaderOld of(ReferenceData refData) {
	    return new TradeCsvLoaderOld(TradeCsvInfoResolverPolicy.of(refData));
	  }

	  /**
	   * Obtains an instance that uses the specified resolver for additional information.
	   * 
	   * @param resolver  the resolver used to parse additional information
	   * @return the loader
	   */
	  public static TradeCsvLoaderOld of(TradeCsvInfoResolverPolicy resolver) {
	    return new TradeCsvLoaderOld(resolver);
	  }

	  // restricted constructor
	  private TradeCsvLoaderOld(TradeCsvInfoResolverPolicy tradeCsvInfoResolver) {
	    this.resolver = ArgChecker.notNull(tradeCsvInfoResolver, "resolver");
	  }

	  //-------------------------------------------------------------------------
	  /**
	   * Loads one or more CSV format trade files.
	   * <p>
	   * CSV files sometimes contain a Unicode Byte Order Mark.
	   * This method uses {@link UnicodeBom} to interpret it.
	   * 
	   * @param resources  the CSV resources
	   * @return the loaded trades, trade-level errors are captured in the result
	   */
	  public ValueWithFailures<List<Trade>> load(ResourceLocator... resources) {
	    return load(Arrays.asList(resources));
	  }

	  /**
	   * Loads one or more CSV format trade files.
	   * <p>
	   * CSV files sometimes contain a Unicode Byte Order Mark.
	   * This method uses {@link UnicodeBom} to interpret it.
	   * 
	   * @param resources  the CSV resources
	   * @return the loaded trades, all errors are captured in the result
	   */
	  public ValueWithFailures<List<Trade>> load(Collection<ResourceLocator> resources) {
	    Collection<CharSource> charSources = resources.stream()
	        .map(r -> r.getByteSource().asCharSourceUtf8UsingBom())
	        .collect(toList());
	    return parse(charSources);
	  }

	  //-------------------------------------------------------------------------
	  /**
	   * Checks whether the source is a CSV format trade file.
	   * <p>
	   * This parses the headers as CSV and checks that mandatory headers are present.
	   * This is determined entirely from the 'Strata Trade Type' column.
	   * 
	   * @param charSource  the CSV character source to check
	   * @return true if the source is a CSV file with known headers, false otherwise
	   */
	  public boolean isKnownFormat(CharSource charSource) {
	    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
	      return csv.containsHeader(TRADE_TYPE_FIELD);
	    } catch (RuntimeException ex) {
	      return false;
	    }
	  }

	  //-------------------------------------------------------------------------
	  /**
	   * Parses one or more CSV format trade files.
	   * <p>
	   * CSV files sometimes contain a Unicode Byte Order Mark.
	   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
	   * 
	   * @param charSources  the CSV character sources
	   * @return the loaded trades, all errors are captured in the result
	   */
	  public ValueWithFailures<List<Trade>> parse(Collection<CharSource> charSources) {
	    return parse(charSources, Trade.class);
	  }

	  /**
	   * Parses one or more CSV format trade files with an error-creating type filter.
	   * <p>
	   * A list of types is specified to filter the trades.
	   * Trades that do not match the type will be included in the failure list.
	   * <p>
	   * CSV files sometimes contain a Unicode Byte Order Mark.
	   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
	   * 
	   * @param charSources  the CSV character sources
	   * @param tradeTypes  the trade types to return
	   * @return the loaded trades, all errors are captured in the result
	   */
	  public ValueWithFailures<List<Trade>> parse(
	      Collection<CharSource> charSources,
	      List<Class<? extends Trade>> tradeTypes) {

	    ValueWithFailures<List<Trade>> parsed = parse(charSources, Trade.class);
	    List<Trade> valid = new ArrayList<>();
	    List<FailureItem> failures = new ArrayList<>(parsed.getFailures());
	    for (Trade trade : parsed.getValue()) {
	      if (tradeTypes.contains(trade.getClass())) {
	        valid.add(trade);
	      } else {
	        failures.add(FailureItem.of(
	            FailureReason.PARSING,
	            "Trade type not allowed {tradeType}, only these types are supported: {}",
	            trade.getClass().getName(),
	            tradeTypes.stream().map(t -> t.getSimpleName()).collect(joining(", "))));
	      }
	    }
	    return ValueWithFailures.of(valid, failures);
	  }

	  /**
	   * Parses one or more CSV format trade files with a quiet type filter.
	   * <p>
	   * A type is specified to filter the trades.
	   * Trades that do not match the type are silently dropped.
	   * <p>
	   * CSV files sometimes contain a Unicode Byte Order Mark.
	   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
	   * 
	   * @param <T>  the trade type
	   * @param charSources  the CSV character sources
	   * @param tradeType  the trade type to return
	   * @return the loaded trades, all errors are captured in the result
	   */
	  public <T extends Trade> ValueWithFailures<List<T>> parse(Collection<CharSource> charSources, Class<T> tradeType) {
	    try {
	      ValueWithFailures<List<T>> result = ValueWithFailures.of(ImmutableList.of());
	      for (CharSource charSource : charSources) {
	        ValueWithFailures<List<T>> singleResult = parseFile(charSource, tradeType);
	        result = result.combinedWith(singleResult, Guavate::concatToList);
	      }
	      return result;

	    } catch (RuntimeException ex) {
	      return ValueWithFailures.of(ImmutableList.of(), FailureItem.of(FailureReason.ERROR, ex));
	    }
	  }

	  // loads a single CSV file, filtering by trade type
	  private <T extends Trade> ValueWithFailures<List<T>> parseFile(CharSource charSource, Class<T> tradeType) {
	    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
	      if (!csv.headers().contains(TRADE_TYPE_FIELD)) {
	        return ValueWithFailures.of(
	            ImmutableList.of(),
	            FailureItem.of(FailureReason.PARSING, "CSV file does not contain '{header}' header: {}", TRADE_TYPE_FIELD,
	                charSource));
	      }
	      return parseFile(csv, tradeType);

	    } catch (RuntimeException ex) {
	      return ValueWithFailures.of(
	          ImmutableList.of(),
	          FailureItem.of(
	              FailureReason.PARSING, ex, "CSV file could not be parsed: {exceptionMessage}: {}", ex.getMessage(), charSource));
	    }
	  }

	  // loads a single CSV file
	  @SuppressWarnings("unchecked")
	  private <T extends Trade> ValueWithFailures<List<T>> parseFile(CsvIterator csv, Class<T> tradeType) {
	    List<T> trades = new ArrayList<>();
	    List<FailureItem> failures = new ArrayList<>();
	    rows:
	    for (CsvRow row : csv.asIterable()) {
	      String typeRaw = row.findField(TRADE_TYPE_FIELD).orElse("");
	      String typeUpper = typeRaw.toUpperCase(Locale.ENGLISH);
	      try {
	        TradeInfo info = parseTradeInfo(row);
	        // allow type matching to be overridden
	        Optional<Trade> overrideOpt = resolver.overrideParseTrade(typeUpper, row, info);
	        if (overrideOpt.isPresent()) {
	          if (tradeType.isInstance(overrideOpt.get())) {
	            trades.add(tradeType.cast(overrideOpt.get()));
	          }
	          continue rows;
	        }
	        // standard type matching
	        TradeCsvParserPlugin plugin = PLUGINS.get(typeUpper);
	        if (plugin != null) {
	          List<CsvRow> additionalRows = new ArrayList<>();
	          while (csv.hasNext() && plugin.isAdditionalRow(row, csv.peek())) {
	            additionalRows.add(csv.next());
	          }
	          plugin.parseTrade(tradeType, row, additionalRows, info, resolver)
	              .filter(parsed -> tradeType.isInstance(parsed))
	              .ifPresent(parsed -> trades.add((T) parsed));
	          continue rows;
	        }
	        // match type using the resolver
	        Optional<Trade> parsedOpt = resolver.parseOtherTrade(typeUpper, row, info);
	        if (parsedOpt.isPresent()) {
	          if (tradeType.isInstance(parsedOpt.get())) {
	            trades.add(tradeType.cast(parsedOpt.get()));
	          }
	          continue rows;
	        }
	        // better error for VARIABLE
	        if (typeUpper.equals("VARIABLE")) {
	          failures.add(FailureItem.of(
	              FailureReason.PARSING,
	              "CSV file contained a 'Variable' type at line {lineNumber} that was not preceeded by a 'Swap' or 'Swaption'",
	              row.lineNumber()));
	        } else {
	          // failed to find the type
	          failures.add(FailureItem.of(
	              FailureReason.PARSING,
	              "CSV trade file type '{tradeType}' is not known at line {lineNumber}",
	              typeRaw,
	              row.lineNumber()));
	        }

	      } catch (RuntimeException ex) {
	        failures.add(FailureItem.of(
	            FailureReason.PARSING,
	            ex,
	            "CSV trade file type '{tradeType}' could not be parsed at line {lineNumber}: {exceptionMessage}",
	            typeRaw,
	            row.lineNumber(),
	            ex.getMessage()));
	      }
	    }
	    return ValueWithFailures.of(trades, failures);
	  }

	  // parse the trade info
	  private TradeInfo parseTradeInfo(CsvRow row) {
	    TradeInfoBuilder infoBuilder = TradeInfo.builder();
	    String scheme = row.findField(ID_SCHEME_FIELD).orElse(DEFAULT_TRADE_SCHEME);
	    row.findValue(ID_FIELD).ifPresent(id -> infoBuilder.id(StandardId.of(scheme, id)));
	    String schemeCpty = row.findValue(CPTY_SCHEME_FIELD).orElse(DEFAULT_CPTY_SCHEME);
	    row.findValue(CPTY_FIELD).ifPresent(cpty -> infoBuilder.counterparty(StandardId.of(schemeCpty, cpty)));
	    row.findValue(TRADE_DATE_FIELD).ifPresent(dateStr -> infoBuilder.tradeDate(LoaderUtils.parseDate(dateStr)));
	    row.findValue(TRADE_TIME_FIELD).ifPresent(timeStr -> infoBuilder.tradeTime(LoaderUtils.parseTime(timeStr)));
	    row.findValue(TRADE_ZONE_FIELD).ifPresent(zoneStr -> infoBuilder.zone(ZoneId.of(zoneStr)));
	    row.findValue(SETTLEMENT_DATE_FIELD)
	        .ifPresent(dateStr -> infoBuilder.settlementDate(LoaderUtils.parseDate(dateStr)));
	    resolver.parseStandardAttributes(row, infoBuilder);
	    resolver.parseTradeInfo(row, infoBuilder);
	    return infoBuilder.build();
	  }

	}

