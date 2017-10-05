
/**
 * The main Niord Proxy Services.
 */

angular.module('niord.proxy.app')

    /**
     * Interface for common functionality
     */
    .factory('AppService', [ '$rootScope', '$window', '$http', '$translate',
        function($rootScope, $window, $http, $translate) {
            'use strict';

            /** Translates the given key **/
            function translate(key, params, language) {
                language = language || $rootScope.language;
                return $translate.instant(key, params, null, language);
            }

            return {

                /** Returns the execution mode **/
                getExecutionMode: function () {
                    return $rootScope.executionMode;
                },


                /** Returns the languages, as defined in the site-config.js configuration file **/
                getLanguages: function () {
                    return $rootScope.languages;
                },


                /** Init the language, e.g. from a request parameter **/
                initLanguage: function (lang) {
                    lang = lang || $window.localStorage['language'];
                    if (lang === undefined && $rootScope.languages && $rootScope.languages.length > 0) {
                        lang = $rootScope.languages[0];
                    }
                    if (lang === undefined) {
                        lang = 'en';

                    }
                    this.setLanguage(lang);
                },


                /** Returns the currently selected language **/
                getLanguage: function () {
                    return $rootScope.language || $window.localStorage['language'] || 'en';
                },


                /**
                 * Registers the current language
                 * @param lang the language
                 */
                setLanguage: function (lang) {
                    if (lang !== $rootScope.language) {
                        $translate.use(lang);
                        $rootScope.language = lang;
                        $window.localStorage['language'] = lang;
                        try {
                            moment.locale(lang);
                        } catch (ex) {
                        }
                    }
                },


                /** Translates the given key **/
                translate: function (key, params, language) {
                    return translate(key, params, language);
                }
            };
        }])


    /**
     * Interface for message-related functionality
     */
    .factory('MessageService', [ '$rootScope', '$http', '$uibModal', 'AppService',
        function($rootScope, $http, $uibModal, AppService) {
            'use strict';

            /** Returns the list of message ids **/
            function extractMessageIds(messages) {
                var ids = [];
                if (messages) {
                    for (var i = 0; i < messages.length; i++) {
                        ids.push(messages[i].id);
                    }
                }
                return ids;
            }

            return {

                /** Returns the area roots **/
                getAreaRoots: function () {
                    return $http.get('/rest/messages/area-roots');
                },


                /** Returns the root area for the given area **/
                rootArea: function (area) {
                    while (area && area.parent) {
                        area = area.parent;
                    }
                    return area;
                },


                /** Returns the description record for the given language **/
                desc: function(o, lang) {
                    lang = lang || $rootScope.language;
                    if (o && o.descs && o.descs.length > 0) {
                        for (var x = 0; x < o.descs.length; x++) {
                            if (o.descs[x].lang === lang) {
                                return o.descs[x];
                            }
                        }
                        return o.descs[0];
                    }
                    return undefined;
                },


                /** Generate the HTML to display as a message ID badge **/
                messageIdLabelHtml : function (msg, showStatus) {

                    if (!msg) {
                        return '';
                    }

                    var label = msg.shortId ? msg.shortId : undefined;
                    var messageClass = msg.mainType === 'NW' ? 'label-message-nw' : 'label-message-nm';

                    // If no shortId is defined, show the message type instead
                    if (!label) {
                        label = msg.type ? AppService.translate('TYPE_' + msg.type) + ' ' : '';
                        label += msg.mainType ? AppService.translate('MAIN_TYPE_' + msg.mainType) : '';
                    }
                    label = '<span class="' + messageClass + '">' + label + '</span>';

                    // If requested, show cancelled and expired status of the message
                    // Special case: Permanent NtMs may expire, but the hazard/event they pertain to is still valid,
                    //               so, do not show the expired badge for these.
                    if (showStatus && ((msg.status === 'EXPIRED' && msg.type !== 'PERMANENT_NOTICE') || msg.status === 'CANCELLED')) {
                        label += '<span class="label-message-status">'
                            + AppService.translate('STATUS_' + msg.status)
                            + '</span>';
                    }

                    // For NM T&P add a suffix
                    var suffix = '';
                    if (msg.type === 'TEMPORARY_NOTICE' || msg.type === 'PRELIMINARY_NOTICE') {
                        suffix = msg.type === 'TEMPORARY_NOTICE' ? '&nbsp; (T)' : '&nbsp; (P)';
                    }

                    return label + suffix;
                },


                /** Returns the features associated with a message **/
                featuresForMessage: function(msg) {
                    var features = [];
                    if (msg && msg.parts && msg.parts.length) {
                        angular.forEach(msg.parts, function (part) {
                            if (part.geometry && part.geometry.features && part.geometry.features.length) {
                                features.push.apply(features, part.geometry.features);
                            }
                        });
                    }
                    return features;
                },


                /** Opens a message details dialog **/
                detailsDialog: function(messageId, messages) {
                    return $uibModal.open({
                        controller: "MessageDialogCtrl",
                        templateUrl: "/app/message-details-dialog.html",
                        size: 'lg',
                        resolve: {
                            messageId: function () {
                                return messageId;
                            },
                            messages: function () {
                                return messages && messages.length > 0 ? extractMessageIds(messages) : [ messageId ];
                            }
                        }
                    });
                },


                /** Returns the message filters */
                search: function(params) {
                    return $http.get('/rest/messages/search?' + params);
                },


                details: function (id) {
                    return $http.get('/rest/messages/message/' + encodeURIComponent(id)
                                + '?language=' + $rootScope.language);
                }
            };
        }])


    /**
     * Interface for logging to Google Analytics
     */
    .factory('AnalyticsService', [ '$rootScope', '$window', '$location',
        function($rootScope, $window, $location) {
            'use strict';

            function gaEnabled() {
                return $rootScope.analyticsTrackingId && $rootScope.analyticsTrackingId.length > 0;
            }

            return {

                /** Returns if Google Analytics is enabled or not **/
                enabled: function () {
                    return gaEnabled();
                },


                /** Initializes Google Analytics **/
                initAnalytics: function () {
                    // initialise google analytics
                    if (gaEnabled()) {
                        try {
                            $window.ga('create', $rootScope.analyticsTrackingId, 'auto');
                        } catch (ex) {
                        }
                    }
                },


                /** Logs the given page view **/
                logPageView: function (page) {
                    if (gaEnabled()) {
                        try {
                            page = page || $location.path();
                            $window.ga('send', 'pageview', page);
                        } catch (ex) {
                        }
                    }
                },


                /** Logs the given event **/
                logEvent: function (category, action, label, value) {
                    if (gaEnabled()) {
                        try {
                            $window.ga('send', 'event', category, action, label, value);
                        } catch (ex) {
                        }
                    }
                }
            };
        }])


    /**
     * The modalService is very much inspired by (even copied from):
     * http://weblogs.asp.net/dwahlin/building-an-angularjs-modal-service
     */
    .service('DialogService', ['$uibModal',
        function ($uibModal) {
            'use strict';

            var modalDefaults = {
                backdrop: true,
                keyboard: true,
                modalFade: true,
                templateUrl: '/app/dialog.html'
            };

            var modalOptions = {
                closeButtonText: 'Cancel',
                actionButtonText: 'OK',
                headerText: '',
                bodyText: undefined
            };


            /** Display a dialog with the given options */
            this.showDialog = function (customModalDefaults, customModalOptions) {
                if (!customModalDefaults) {
                    customModalDefaults = {};
                }
                customModalDefaults.backdrop = 'static';
                return this.show(customModalDefaults, customModalOptions);
            };


            /** Displays a confimation dialog */
            this.showConfirmDialog = function (headerText, bodyText) {
                return this.showDialog(undefined, {headerText: headerText, bodyText: bodyText});
            };


            /** Opens the dialog with the given options */
            this.show = function (customModalDefaults, customModalOptions) {
                //Create temp objects to work with since we're in a singleton service
                var tempModalDefaults = {};
                var tempModalOptions = {};

                //Map angular-ui modal custom defaults to modal defaults defined in service
                angular.extend(tempModalDefaults, modalDefaults, customModalDefaults);

                //Map modal.html $scope custom properties to defaults defined in service
                angular.extend(tempModalOptions, modalOptions, customModalOptions);

                if (!tempModalDefaults.controller) {
                    tempModalDefaults.controller = function ($scope, $uibModalInstance) {
                        $scope.modalOptions = tempModalOptions;
                        $scope.modalOptions.ok = function (result) {
                            $uibModalInstance.close(result);
                        };
                        $scope.modalOptions.close = function () {
                            $uibModalInstance.dismiss('cancel');
                        };
                    }
                }

                return $uibModal.open(tempModalDefaults).result;
            };

        }])




    /**
     * The language service is used for changing language, etc.
     */
    .service('MapService', ['$rootScope', function ($rootScope) {
            'use strict';

            var projMercator = 'EPSG:3857';
            var proj4326 = 'EPSG:4326';
            var geoJsonFormat = new ol.format.GeoJSON();


            /** Returns the data projection */
            this.dataProjection = function () {
                return proj4326;
            };


            /** Returns the feature projection */
            this.featureProjection = function () {
                return projMercator;
            };


            /** Rounds each value of the array to the given number of decimals */
            this.round = function (values, decimals) {
                for (var x = 0; values && x < values.length; x++) {
                    // NB: Prepending a '+' will convert from string to float
                    values[x] = +values[x].toFixed(decimals);
                }
                return values;
            };


            /** Converts lon-lat array to xy array in mercator */
            this.fromLonLat = function(lonLat) {
                return lonLat ? ol.proj.fromLonLat(lonLat) : null;
            };


            /** Converts xy array in mercator to a lon-lat array */
            this.toLonLat = function(xy) {
                return xy ? ol.proj.transform(xy, projMercator, proj4326) : null;
            };


            /** Returns the center of the extent */
            this.getExtentCenter = function (extent) {
                var x = extent[0] + (extent[2]-extent[0]) / 2.0;
                var y = extent[1] + (extent[3]-extent[1]) / 2.0;
                return [x, y];
            };


            /** Returns a "sensible" center point of the geometry. Used e.g. for placing labels **/
            this.getGeometryCenter = function (g) {
                var point;
                try {
                    switch (g.getType()) {
                        case 'MultiPolygon':
                            var poly = g.getPolygons().reduce(function(left, right) {
                                return left.getArea() > right.getArea() ? left : right;
                            });
                            point = poly.getInteriorPoint().getCoordinates();
                            break;
                        case 'MultiLineString':
                            var lineString = g.getLineStrings().reduce(function(left, right) {
                                return left.getLength() > right.getLength() ? left : right;
                            });
                            point = this.getExtentCenter(lineString.getExtent());
                            break;
                        case 'Polygon':
                            point = g.getInteriorPoint().getCoordinates();
                            break;
                        case 'Point':
                            point = g.getCoordinates();
                            break;
                        case 'LineString':
                        case 'MultiPoint':
                        case 'GeometryCollection':
                            point = this.getExtentCenter(g.getExtent());
                            break;
                    }
                } catch (ex) {
                }
                return point;
            };


            /** Computes the center for the list of features **/
            this.getFeaturesCenter = function (features) {
                var extent = ol.extent.createEmpty();
                for (var i = 0; features && i < features.length; i++) {
                    var geometry = features[i].getGeometry();
                    if (geometry) {
                        ol.extent.extend(extent, geometry.getExtent());
                    }
                }
                return ol.extent.isEmpty(extent) ? null : ol.extent.getCenter(extent);
            };


            /** Converts a GeoJSON feature to an OL feature **/
            this.gjToOlFeature = function (feature) {
                return geoJsonFormat.readFeature(feature, {
                    dataProjection: proj4326,
                    featureProjection: projMercator
                });
            };


        /**
         * Serializes the "readable" coordinates of a geometry
         * <p>
         * When serializing coordinates, adhere to a couple of rules:
         * <ul>
         *     <li>If the "parentFeatureIds" feature property is defined, skip the coordinates.</li>
         *     <li>If the "restriction" feature property has the value "affected", skip the coordinates.</li>
         *     <li>For polygon linear rings, skip the last coordinate (which is identical to the first).</li>
         *     <li>For (multi-)polygons, only include the exterior ring, not the interior ring.</li>
         * </ul>
         */
        this.serializeReadableCoordinates = function (g, coords, props, index, polygonType) {
            var that = this;
            props = props || {};
            index = index || 0;
            if (g) {
                if (g instanceof Array) {
                    if (g.length >= 2 && $.isNumeric(g[0])) {
                        var bufferFeature = props['parentFeatureIds'];
                        var affectedArea = props['restriction'] === 'affected';
                        var includeCoord = (polygonType !== 'Exterior');
                        if (includeCoord && !bufferFeature && !affectedArea) {
                            coords.push({
                                lon: g[0],
                                lat: g[1],
                                index: index,
                                name: props['name:' + index + ':' + $rootScope.language]
                            });
                        }
                        index++;
                    } else {
                        for (var x1 = 0; x1 < g.length; x1++) {
                            polygonType = (polygonType === 'Interior' && x1 === g.length - 1) ? 'Exterior' : polygonType;
                            index = that.serializeReadableCoordinates(g[x1], coords, props, index, polygonType);
                        }
                    }
                } else if (g.type === 'FeatureCollection') {
                    for (var x2 = 0; g.features && x2 < g.features.length; x2++) {
                        index = that.serializeReadableCoordinates(g.features[x2], coords);
                    }
                } else if (g.type === 'Feature') {
                    index = that.serializeReadableCoordinates(g.geometry, coords, g.properties, 0);
                } else if (g.type === 'GeometryCollection') {
                    for (var x3 = 0; g.geometries && x3 < g.geometries.length; x3++) {
                        index = that.serializeReadableCoordinates(g.geometries[x3], coords, props, index);
                    }
                } else if (g.type === 'MultiPolygon') {
                    for (var p = 0; p < g.coordinates.length; p++) {
                        // For polygons, do not include coordinates for interior rings
                        for (var x4 = 0; x4 < g.coordinates[p].length; x4++) {
                            index = that.serializeReadableCoordinates(g.coordinates[p][x4], coords, props, index, x4 === 0 ? 'Interior' : 'Exterior');
                        }
                    }
                } else if (g.type === 'Polygon') {
                    // For polygons, do not include coordinates for interior rings
                    for (var x5 = 0; x5 < g.coordinates.length; x5++) {
                        index = that.serializeReadableCoordinates(g.coordinates[x5], coords, props, index, x5 === 0 ? 'Interior' : 'Exterior');
                    }
                } else if (g.type) {
                    index = that.serializeReadableCoordinates(g.coordinates, coords, props, index);
                }
            }
            return index;
        };

    }]);

