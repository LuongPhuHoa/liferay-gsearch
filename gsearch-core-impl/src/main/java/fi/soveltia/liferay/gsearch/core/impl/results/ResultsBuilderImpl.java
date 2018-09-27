
package fi.soveltia.liferay.gsearch.core.impl.results;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.facet.Facet;
import com.liferay.portal.kernel.search.facet.collector.FacetCollector;
import com.liferay.portal.kernel.search.facet.collector.TermCollector;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import fi.soveltia.liferay.gsearch.core.api.facet.translator.FacetTranslator;
import fi.soveltia.liferay.gsearch.core.api.facet.translator.FacetTranslatorFactory;
import fi.soveltia.liferay.gsearch.core.api.params.QueryParams;
import fi.soveltia.liferay.gsearch.core.api.results.ResultsBuilder;
import fi.soveltia.liferay.gsearch.core.api.results.item.ResultItemBuilder;
import fi.soveltia.liferay.gsearch.core.api.results.item.ResultItemBuilderFactory;
import fi.soveltia.liferay.gsearch.core.api.results.item.processor.ResultItemProcessor;
import fi.soveltia.liferay.gsearch.core.impl.configuration.ModuleConfiguration;

/**
 * Results builder implementation. Localization is here local because of
 * https://issues.liferay.com/browse/LPS-75141 Move to web module when fixed.
 *
 * @author Petteri Karttunen
 */
@Component(
	configurationPid = "fi.soveltia.liferay.gsearch.core.configuration.GSearchCore",
	immediate = true,
	service = ResultsBuilder.class
)
public class ResultsBuilderImpl implements ResultsBuilder {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JSONObject buildResults(
		PortletRequest portletRequest, PortletResponse portletResponse,
		QueryParams queryParams, SearchContext searchContext, Hits hits) {

		_hits = hits;
		_portletRequest = portletRequest;
		_portletResponse = portletResponse;
		_queryParams = queryParams;

		// See class comments

		_resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", _queryParams.getLocale(),
			ResultsBuilderImpl.class);

		JSONObject resultsObject = JSONFactoryUtil.createJSONObject();

		long startTime = System.currentTimeMillis();

		// Create items array

		resultsObject.put("items", createItemsArray());

		// Create meta info array

		resultsObject.put("meta", createMetaObject(searchContext));

		// Paging object

		resultsObject.put("paging", createPagingObject());


		if (_log.isDebugEnabled()) {
			_log.debug(
				"Results processing took: " +
					(System.currentTimeMillis() - startTime));
		}
		return resultsObject;
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {

		_moduleConfiguration = ConfigurableUtil.createConfigurable(
			ModuleConfiguration.class, properties);
	}

	/**
	 * Add result item processor to the list.
	 *
	 * @param resultItemProcessor
	 */
	protected void addResultItemProcessor(
		ResultItemProcessor resultItemProcessor) {

		if (_resultItemProcessors == null) {
			_resultItemProcessors = new ArrayList<ResultItemProcessor>();
		}
		_resultItemProcessors.add(resultItemProcessor);
	}

	/**
	 * Create facets array for the results.
	 *
	 * @param searchContext
	 * @return facets as JSON array
	 * @throws Exception
	 */
	protected JSONArray createFacetsArray(SearchContext searchContext)
		throws Exception {

		// Get facets configuration

		JSONArray configuration = JSONFactoryUtil.createJSONArray(
			_moduleConfiguration.facetConfiguration());

		// Get facets.

		JSONArray resultArray = JSONFactoryUtil.createJSONArray();

		Map<String, Facet> facets = searchContext.getFacets();

		List<Facet> facetsList = sortFacetList(
			ListUtil.fromCollection(facets.values()), configuration);

		for (Facet facet : facetsList) {

			if (facet.isStatic()) {
				continue;
			}

			// Get single facet configuration

			JSONObject facetConfiguration = null;

			for (int i = 0; i < configuration.length(); i++) {
				if (facet.getFieldName().equals(
					configuration.getJSONObject(i).get("fieldName"))) {
					facetConfiguration = configuration.getJSONObject(i);
					break;
				}
			}

			FacetCollector facetCollector = facet.getFacetCollector();

			JSONArray termArray = JSONFactoryUtil.createJSONArray();

			// Process facets

			FacetTranslator translator =
				_facetTranslatorFactory.getTranslator(facet.getFieldName());

			if (translator != null) {
				termArray = translator.translateValues(
					_queryParams, facetCollector, facetConfiguration);

			}
			else {

				List<TermCollector> termCollectors =
					facetCollector.getTermCollectors();

				for (TermCollector tc : termCollectors) {

					JSONObject item = JSONFactoryUtil.createJSONObject();

					item.put("frequency", tc.getFrequency());
					item.put("name", tc.getTerm());
					item.put("term", tc.getTerm());

					termArray.put(item);
				}
			}

			// Put item to array (if items found)

			if (termArray.length() > 0) {
				JSONObject resultItem = JSONFactoryUtil.createJSONObject();

				// Putting anyoption here because of localization issue. Please
				// see class comments.

				resultItem.put(
					"anyOption", getLocalization(
						"any-" + facetCollector.getFieldName().toLowerCase()));
				resultItem.put(
						"multipleOption", getLocalization(
							"multiple-" + facetCollector.getFieldName().toLowerCase()));
				resultItem.put(
					"paramName", facetConfiguration.get("paramName"));
				resultItem.put("icon", facetConfiguration.get("icon"));
				resultItem.put("values", termArray);
				resultItem.put("isMultiValued", facetConfiguration.get("isMultiValued"));

				resultArray.put(resultItem);
			}
		}
		return resultArray;
	}

	/**
	 * Create array of result items as JSON array.
	 *
	 * @return JSON array of result items
	 */
	protected JSONArray createItemsArray() {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		Document[] docs = _hits.getDocs();

		if (_hits == null || docs.length == 0) {
			return jsonArray;
		}

		// Loop through search result documents and create the
		// JSON array of items to be delivered for UI

		for (int i = 0; i < docs.length; i++) {

			Document document = docs[i];

			try {

				if (_log.isDebugEnabled()) {
					_log.debug(
						"##############################################");

					_log.debug("Score: " + _hits.getScores()[i]);

					for (Entry<String, Field> e : document.getFields().entrySet()) {
						_log.debug(e.getKey() + ":" + e.getValue().getValue());
					}
				}

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				// Get item type specific item result builder

				ResultItemBuilder resultItemBuilder =
					_resultsBuilderFactory.getResultBuilder(
						_portletRequest, _portletResponse, document,
						_moduleConfiguration.assetPublisherPage());

				// Title

				jsonObject.put("title", resultItemBuilder.getTitle());

				// Date

				jsonObject.put("date", resultItemBuilder.getDate());

				// Description

				jsonObject.put(
					"description", resultItemBuilder.getDescription());

				// Image src

				if (document.get(Field.ENTRY_CLASS_NAME).equals(
					DLFileEntry.class.getName())) {

					jsonObject.put("imageSrc", resultItemBuilder.getImageSrc());
				}

				// Type

				jsonObject.put(
					"type",
					getLocalization("type." + resultItemBuilder.getType().toLowerCase()));

				// Link

				jsonObject.put("link", resultItemBuilder.getLink());


				jsonObject.put("breadcrumbs", resultItemBuilder.getBreadcrumbs());

				// Tags

				String[] tags = resultItemBuilder.getTags();

				if (tags != null && tags.length > 0 && tags[0].length() > 0) {

					jsonObject.put("tags", tags);
				}

				String[] categories = resultItemBuilder.getCategories();

				if (categories != null && categories.length > 0 && categories[0].length() > 0) {

					jsonObject.put("categories", categories);
				}



				// Additional metadata

				jsonObject.put("metadata", resultItemBuilder.getMetadata());

				// Execute result item processors

				executeResultItemProcessors(document, jsonObject);

				// Put single item to result array

				jsonArray.put(jsonObject);

			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}

		return jsonArray;
	}

	/**
	 * Create meta information object of the results.
	 *
	 * @return meta information JSON object
	 */
	protected JSONObject createMetaObject(SearchContext searchContext) {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("resultsLayout", _queryParams.getResultsLayout());

		// If this parameter is populated, there was an alternate search.

		String originalQueryTerms = _queryParams.getOriginalKeywords();

		if (originalQueryTerms != null) {
			jsonObject.put("originalQueryTerms", originalQueryTerms);
		}

		jsonObject.put("queryTerms", _queryParams.getKeywords());

		jsonObject.put(
			"executionTime", String.format("%.3f", _hits.getSearchTime()));
		jsonObject.put("querySuggestions", _hits.getQuerySuggestions());


		jsonObject.put("start", getStart());

		jsonObject.put("end", getEnd());

		jsonObject.put("totalPages", getPageCount());

		jsonObject.put("totalHits", _hits.getLength());

		jsonObject.put("typeCounts", getTypeCounts(searchContext));

		return jsonObject;
	}

	private JSONObject getTypeCounts(SearchContext searchContext) {

		JSONObject typeCounts = JSONFactoryUtil.createJSONObject();
		try {
			JSONArray configuration = JSONFactoryUtil.createJSONArray(
				_moduleConfiguration.typeConfiguration());

			Map<String, Facet> facets = searchContext.getFacets();

			for (int i = 0; i < configuration.length(); i++) {
				JSONObject entry = configuration.getJSONObject(i);
				String mainFacetKey = "";
				String term = "";
				String typeKey = entry.getString("key");
				if (entry.has("ddmStructureKey")) {
					mainFacetKey = "ddmStructureKey";
					term = entry.getString("ddmStructureKey");
				} else {
					mainFacetKey = "entryClassName";
					term = entry.getString("entryClassName");
				}
				if (facets.containsKey(mainFacetKey)) {
					Facet mainFacet = facets.get(mainFacetKey);
					TermCollector termCollector = mainFacet.getFacetCollector().getTermCollector(term);
					typeCounts.put(typeKey, termCollector.getFrequency());
				}
			}
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		return typeCounts;
	}

	private int getEnd() {
		return Math.min(_queryParams.getPageSize() * getCurrentPage(_queryParams.getPageSize(), getStart()), _hits.getLength());
	}

	/**
	 * Create paging object.
	 *
	 * @return paging JSON object
	 */
	protected JSONObject createPagingObject() {

		JSONObject pagingObject = JSONFactoryUtil.createJSONObject();

		int totalHits = _hits.getLength();

		if (totalHits == 0) {
			return pagingObject;
		}

		// Count of pages to show at once in the paging bar.

		int pagesToShow = 10;
		int pageSize = _queryParams.getPageSize();
		int start = getStart();
		int pageCount = getPageCount();

		// Page number to start from.

		int currentPage = getCurrentPage(pageSize, start);
		pagingObject.put("currentPage", currentPage);

		// Page number to start to loop from.

		int loopStart = 1;

		// Page number to loop to.

		int loopEnd = pagesToShow;

		if (currentPage > pagesToShow) {
			loopStart = currentPage - (pagesToShow / 2);
			loopEnd = currentPage + (pagesToShow / 2);
		}

		if (loopEnd > pageCount) {
			loopEnd = pageCount;
		}

		// Previous and next buttons.

		int prevStart = -1;

		if (currentPage > pagesToShow) {
			prevStart = (loopStart - 2) * pageSize;
			pagingObject.put("prevStart", prevStart);
		}

		int nextStart = -1;

		if (pageCount > loopEnd) {
			nextStart = loopEnd * pageSize;
			pagingObject.put("nextStart", nextStart);
		}

		// Create paging set.

		JSONArray pageArray = JSONFactoryUtil.createJSONArray();

		int prevPageStart = -1;
		int nextPageStart = -1;

		for (int i = loopStart; i <= loopEnd; i++) {

			JSONObject pageObject = JSONFactoryUtil.createJSONObject();

			pageObject.put("number", i);
			pageObject.put("start", (i - 1) * pageSize);

			if (i == currentPage) {
				pageObject.put("selected", true);
				prevPageStart = (i - 2) * pageSize;
				if (currentPage < pageCount) {
					nextPageStart = i * pageSize;
				}
			}
			pageArray.put(pageObject);
		}

		pagingObject.put("pages", pageArray);

		pagingObject.put("prevPageStart", prevPageStart);
		pagingObject.put("nextPageStart", nextPageStart);

		return pagingObject;
	}

	private int getCurrentPage(int pageSize, int start) {
		return ((int) Math.floor((start + 1) / pageSize)) + 1;
	}

	/**
	 * Execute result item processors.
	 *
	 * @param document
	 * @param resultItem
	 */
	protected void executeResultItemProcessors(
		Document document, JSONObject resultItem) {

		if (_log.isDebugEnabled()) {
			_log.debug("Executing result item processors.");
		}

		if (_resultItemProcessors == null) {
			return;
		}

		for (ResultItemProcessor r : _resultItemProcessors) {

			if (r.isEnabled()) {

				try {
					r.process(document, resultItem);
				}
				catch (Exception e) {
					_log.error(e, e);
				}
			}
			else {

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Processor " + r.getClass().getName() + " is disabled");
				}
			}
		}
	}

	/**
	 * Remove a result item processor from list.
	 *
	 * @param resultItemProcessor
	 */
	protected void removeResultItemProcessor(
		ResultItemProcessor resultItemProcessor) {

		_resultItemProcessors.remove(resultItemProcessor);
	}

	@Reference(unbind = "-")
	protected void setFacetTranslatorFactory(
		FacetTranslatorFactory facetTranslatorFactory) {

		_facetTranslatorFactory = facetTranslatorFactory;
	}

	@Reference(unbind = "-")
	protected void setResultItemBuilderFactory(
		ResultItemBuilderFactory resultsBuilderFactory) {

		_resultsBuilderFactory = resultsBuilderFactory;
	}

	/**
	 * Sort facet list. Use the order of configuration.
	 *
	 * @param facets
	 * @param configuration
	 * @return sorted facet list
	 */
	protected List<Facet> sortFacetList(
		List<Facet> facets, JSONArray configuration) {

		List<Facet> sortedList = new ArrayList<Facet>();

		for (int i = 0; i < configuration.length(); i++) {

			String fieldName =
				configuration.getJSONObject(i).getString("fieldName");

			for (Facet facet : facets) {
				if (fieldName.equals(facet.getFieldName())) {
					sortedList.add(facet);
					break;
				}
			}
		}
		return sortedList;
	}

	/**
	 * Get item type localization. Fall back to key if not found.
	 *
	 * @param key
	 * @return
	 */
	private String getLocalization(String key) {

		try {
			return _resourceBundle.getString(key);
		}
		catch (Exception e) {
			_log.error("Localization value for " + key + " not found.");
		}
		return key;
	}

	/**
	 * Get page count
	 *
	 * @return
	 */
	private int getPageCount() {
		return (int) Math.ceil(
			_hits.getLength() * 1.0 / _queryParams.getPageSize());

	}


	/**
	 * Check start parameter.
	 *
	 * We might get a start parameter higher than hits total.
	 * In that case the last page is returned and start
	 * has to be adjusted.
	 *
	 * @return
	 */
	private int getStart() {

		int pageSize = _queryParams.getPageSize();
		int totalHits = _hits.getLength();
		int start = _queryParams.getStart();

		if (totalHits < start) {

			start = (getPageCount()-1) * pageSize;

			if (start < 0) {
				start = 0;
			}
		}

		return start;
	}

	protected Hits _hits;

	protected PortletRequest _portletRequest;

	protected PortletResponse _portletResponse;

	protected QueryParams _queryParams;

	protected ResourceBundle _resourceBundle;

	private FacetTranslatorFactory _facetTranslatorFactory;

	private volatile ModuleConfiguration _moduleConfiguration;

	private ResultItemBuilderFactory _resultsBuilderFactory;

	@Reference(
		bind = "addResultItemProcessor",
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		service = ResultItemProcessor.class,
		unbind = "removeResultItemProcessor"
	)
	private volatile List<ResultItemProcessor> _resultItemProcessors;

	private static final Log _log =
		LogFactoryUtil.getLog(ResultsBuilderImpl.class);

}
