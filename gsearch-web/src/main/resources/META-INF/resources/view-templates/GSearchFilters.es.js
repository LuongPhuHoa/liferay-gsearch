import Component from 'metal-component/src/Component';
import Soy from 'metal-soy/src/Soy';
import core from 'metal/src/core';
import Ajax from 'metal-ajax/src/Ajax';
import MultiMap from 'metal-multimap/src/MultiMap';

import GSearchUtils from '../js/GSearchUtils.es';

import templates from './GSearchFilters.soy';

/**
 * GSearch filters component.
 */
class GSearchFilters extends Component {

	/**
	 * @inheritDoc
	 */
	constructor(opt_config, opt_parentElement) {

		super(opt_config, opt_parentElement);

		this.debug = opt_config.JSDebugEnabled;

		this.initialQueryParameters = opt_config.initialQueryParameters;

		this.portletNamespace = opt_config.portletNamespace;

		this.language = opt_config.language;

		this.assetTypeOptions = opt_config.assetTypeOptions;

		this.unitFilters = opt_config.unitFilters;
	}

	/**
	 * @inheritDoc
	 */
	attached() {

		if (this.debug) {
			console.log("GSearchFilters.attached()");
		}

		this.setupAssetTypeOptions();

		this.setupUnitFilters();

		// Set initial query parameters from calling url.

		GSearchUtils.setInitialQueryParameters(
			this.initialQueryParameters,
			this.templateParameters,
			this.setQueryParam
		);

		// Setup options lists.
		GSearchUtils.bulkSetupOptionLists(
			this.portletNamespace + 'BasicFilters',
			'optionmenu',
			this.getQueryParam,
			this.setQueryParam
		);

        // Add results callback

		this.addResultsCallback(this.updateAssetTypeFacetCounts);

        this.setupTimeFilterRanges(
        	this.portletNamespace,
			this.language,
			this.setQueryParam,
			this.getQueryParam,
			this.getDateStringForUrlParam,
			this.updateTimeRangeQuery,
			this.bindTimeRangeInput);
	}

	/**
	 * @inheritDoc
	 */
	rendered() {

		if (this.debug) {
			console.log("GSearchFilters.rendered()");
		}
	}

	getDateStringForUrlParam(date) {
        let day = '' + date.getDate();
        day.length === 1 ? day = '0' + day : day;
        let month = '' + (date.getMonth() + 1);
        month.length === 1 ? month = '0' + month : month;
        let year = date.getFullYear();
        return day + '-' + month + '-' + year;
    }


    setupTimeFilterRanges(portletNamespace, language, queryParamSetter, queryParamGetter, getDateString, updateTimeRangeQuery, bindTimeRangeInput) {

        bindTimeRangeInput('timeRangeStart', 'timeStart', portletNamespace, queryParamSetter, getDateString, updateTimeRangeQuery);
        bindTimeRangeInput('timeRangeEnd', 'timeEnd', portletNamespace, queryParamSetter, getDateString, updateTimeRangeQuery);

        AUI({lang: language}).use('aui-datepicker', function(A) {
            new A.DatePicker(
                {
                    trigger: '#' + portletNamespace + 'timeRangeStart',
                    mask: '%d.%m.%Y',
					popover: {
                    	zIndex: 10000
					},
                    popoverCssClass: 'datepicker-popover',
                    on:
					{
                        selectionChange: function (event) {
                            queryParamSetter('timeStart', getDateString(new Date(event.newSelection)), true, false);
                        }
                    }
                }
            );
		});

        AUI({lang: language}).use('aui-datepicker', function(A) {
            new A.DatePicker(
                {
                    trigger: '#' + portletNamespace + 'timeRangeEnd',
                    mask: '%d.%m.%Y',
                    popover: {
                        zIndex: 10000
                    },
                    popoverCssClass: 'datepicker-popover',
                    on: {
                        selectionChange: function(event) {
                            queryParamSetter('timeEnd', getDateString(new Date(event.newSelection)), true, false);
                        }
					}
                }
            );
        });

        let timeParam = queryParamGetter('time', true);
        if ((timeParam !== undefined) && (timeParam !== null) && (timeParam === 'range')) {
            let dateRangeDiv = $('#' + portletNamespace + 'TimeFilterOptions div.time-range');
        	dateRangeDiv.removeClass('hidden');
            let rangeStart = queryParamGetter('timeStart', true);
            let rangeEnd = queryParamGetter('timeEnd', true);
            if ((rangeStart !== undefined) && (rangeStart !== null) && (rangeStart !== '')) {
                rangeStart = rangeStart.replace(/-/g, '.');
                dateRangeDiv.find('#' + portletNamespace + 'timeRangeStart').val(rangeStart);
            }
            if ((rangeEnd !== undefined) && (rangeEnd !== null) && (rangeEnd !== '')) {
                rangeEnd = rangeEnd.replace(/-/g, '.');
                dateRangeDiv.find('#' + portletNamespace + 'timeRangeEnd').val(rangeEnd);
            }
        }
    }

    bindTimeRangeInput(inputId, parameter, portletNamespace, queryParamSetter, getDateString, updateTimeRangeQuery) {
        $('#' + portletNamespace + inputId).on('blur keyup', function(event) {
            if (event.type === 'blur' || event.which === 13) {
                let dateString = $('#' + portletNamespace + inputId).val();
                updateTimeRangeQuery(parameter, dateString, queryParamSetter, getDateString);
            }
        });

    }
    updateTimeRangeQuery(parameter, dateString, queryParamSetter, getDateString) {
        let matches = dateString.match(/\d{1,2}\.\d{1,2}\.\d\d\d\d/);
		if (matches != null) {
            let date = dateString.split('.');
            let day = date[0];
            let month = date[1] - 1;
            let year = date[2];
            if ((day >= 1) &&
                (day <= 31) &&
                (month >= 0) &&
                (month <= 11) &&
                (year >= 1970) &&
                (year <= 2100)) {
                let selection = new Date(year, month, day);
                queryParamSetter(parameter, getDateString(selection), true, false);
            }
        }

    }
	/**
	 * Setup asset type options
	 */
	setupAssetTypeOptions() {

		let html = '';

		let length = this.assetTypeOptions.length;

		for (let i = 0; i < length; i++) {

			let item = this.assetTypeOptions[i];

            let checked = this.initialQueryParameters !== null && this.initialQueryParameters.type !== null && this.initialQueryParameters.type.indexOf(item.key) > -1;

			html += '<li>';
			html += '<label for="' + this.portletNamespace + 'type-' + item.key + '" class="type-selection checkbox">';
			html += '<span class="text">' + item.localization + '</span>';
			html += '<span class="count"></span>';
			html += '<input type="checkbox" id="' + this.portletNamespace + 'type-' + item.key + '" value="' + item.key + '" data-value="' + item.key + '"';
			if (checked) {
                if (item.key !== 'everything') {
                    $('#' + this.portletNamespace + 'TypeFilterOptions' + ' li.default :checkbox').prop('checked', false);
                }
				html += 'checked="checked"';
			}
			html += '/>';
            html += '<span class="checkmark"></span>';
			html += '</li>';
		}
		$('#' + this.portletNamespace + 'TypeFilterOptions').append(html);
	}

	/**
	 * Update asset type facet counts.
	 */
	updateAssetTypeFacetCounts(portletNamespace, results) {

		// Clear current values

		$('#' + portletNamespace + 'TypeFilterOptions li label span.count').html('');

		if (results && results.meta.typeCounts) {

            $('#' + portletNamespace + 'TypeFilterOptions li label input').each(function(element) {
                let key = $(this).attr('data-value');
            	if (key in results.meta.typeCounts) {
					let frequency = results.meta.typeCounts[key];
                    $(this).parent().find('.count').html('(' + frequency + ')');
				}
			});
		}
	}

	setupUnitFilters() {
		this.createUnitFilters(this.unitFilters, $('#' + this.portletNamespace + 'UnitFilterUl'));
	}

	createUnitFilters(units, element) {
		let initialUnitParams = this.initialQueryParameters.unit !== null ? this.initialQueryParameters.unit : null;
        for (let i = 0; i < units.length; i++) {
            let unit = units[i];
            let li = null;
            li = $(document.createElement('li'));

            let label = $('<label />', { 'for': this.portletNamespace + 'unitCategory-' + unit.categoryId, text: unit.name });
            label.addClass('unit-selection checkbox');

            let checkbox = $('<input />', {
                type: 'checkbox',
                id: this.portletNamespace + 'unitCategory-' + unit.categoryId,
                value: unit.name ,
                'data-value': unit.categoryId,
                checked: initialUnitParams !== null && initialUnitParams.indexOf(unit.categoryId) > -1
            });
            checkbox.appendTo(label);

            $('<span class="checkmark"></span>').appendTo(label);

            label.appendTo(li);

            element.append(li);

        }

	}
}

/**
 * State definition.
 * @type {!Object}
 * @static
 */
GSearchFilters.STATE = {
	addResultsCallback: {
		validator: core.isFunction
	},
	getQueryParam: {
		validator: core.isFunction
	},
	setQueryParam: {
		validator: core.isFunction
	},
	templateParameters: {
		value: ['type', 'scope', 'time', 'unit', 'timeStart', 'timeEnd']
	}
};

// Register component

Soy.register(GSearchFilters, templates);

export default GSearchFilters;