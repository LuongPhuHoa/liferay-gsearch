
package fi.soveltia.liferay.gsearch.geolocation.results.item.processor;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.geolocation.GeoLocationPoint;

import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import fi.soveltia.liferay.gsearch.core.api.query.context.QueryContext;
import fi.soveltia.liferay.gsearch.core.api.results.item.ResultItemBuilder;
import fi.soveltia.liferay.gsearch.core.api.results.item.processor.ResultItemProcessor;
import fi.soveltia.liferay.gsearch.geolocation.configuration.ModuleConfiguration;

/**
 * Adds coordinates to result items so that they can be showed on maps result
 * layout.
 * 
 * @author Petteri Karttunen
 */
@Component(
	configurationPid = "fi.soveltia.liferay.gsearch.geolocation.configuration.ModuleConfiguration",
	immediate = true, 
	service = ResultItemProcessor.class
)
public class GeoLocationResultItemProcessor implements ResultItemProcessor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEnabled() {

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(
		PortletRequest portletRequest, PortletResponse portletResponse,
		QueryContext queryParams, Document document,
		ResultItemBuilder resultItemBuilder, JSONObject resultItem)
		throws Exception {

		if (!isEnabled()) {
			return;
		}

		Field geoLocationPointField =
			document.getField(_moduleConfiguration.indexField());

		if (geoLocationPointField == null) {
			return;
		}

		GeoLocationPoint geoLocationPoint =
			geoLocationPointField.getGeoLocationPoint();

		if (geoLocationPoint != null) {

			resultItem.put("latitude", geoLocationPoint.getLatitude());
			resultItem.put("longitude", geoLocationPoint.getLongitude());
		}
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {

		_moduleConfiguration = ConfigurableUtil.createConfigurable(
			ModuleConfiguration.class, properties);
	}

	private volatile ModuleConfiguration _moduleConfiguration;
}
