package fi.soveltia.liferay.gsearch.mini.web.configuration;

import aQute.bnd.annotation.metatype.Meta;

/**
 * GSearch Mini Web module configuration.
 *
 * @author Petteri Karttunen
 *
 */
@Meta.OCD(
	id = "fi.soveltia.liferay.gsearch.mini.web.configuration.GSearchMiniportlet",
	localization = "content/Language",
	name = "GSearch Mini Portlet"
)
public interface ModuleConfiguration {

	@Meta.AD(
		deflt = "/search",
	    name = "search-portlet-page-name",
	    description = "search-portlet-page-desc",
		required = false
	)
	public String searchPortletPage();

	@Meta.AD(
		deflt = "3",
	    name = "keywords-min-length",
		required = false
	)
	public int queryMinLength();

	@Meta.AD(
		deflt = "true",
	    name = "enable-autocompletion-name",
	    description = "enable-autocompletion-desc",
		required = false
	)
	public boolean enableAutoComplete();

	@Meta.AD(
		deflt = "150",
	    description = "autocomplete-delay-desc",
	    name = "autocomplete-delay-name",
		required = false
	)
	public int autoCompleteRequestDelay();

	@Meta.AD(
		deflt = "5",
		description = "search-suggestions-max-count",
		name = "search-suggestions-max-count",
		required = false
	)
	public int searchSuggestionsMaxCount();

	@Meta.AD(
		deflt = "10000",
		description = "request-timeout-desc",
	    name = "request-timeout-name",
		required = false
	)
	public int requestTimeout();
}
