/**
 * The main Niord Proxy app module definition.
 *
 * Define the routes of the single page application.
 */

angular.module('niord.proxy.conf', []);

angular.module('niord.proxy.app',
    [   'ngSanitize',
        'ui.bootstrap',
        'ui.router',
        'pascalprecht.translate',
        'niord.proxy.conf'
    ])

    .config(['$stateProvider', '$urlRouterProvider', '$translateProvider',
        function ($stateProvider, $urlRouterProvider, $translateProvider) {
        'use strict';

            $translateProvider.useSanitizeValueStrategy('sanitize');


            $urlRouterProvider
                .when('/', '/messages/details')
                .otherwise("/");

            $stateProvider
                .state('messages', {
                    url: "/messages",
                    templateUrl: "/app/messages.html",
                    params: {
                        publicationId: { value: '' }
                    }
                })
                .state('messages.map', {
                    url: "/map",
                    templateUrl: "/app/messages-viewmode-map.html"
                })
                .state('messages.table', {
                    url: "/table",
                    templateUrl: "/app/messages-viewmode-table.html"
                })
                .state('messages.details', {
                    url: "/details",
                    templateUrl: "/app/messages-viewmode-details.html"
                })
                .state('messages.details.message', {
                    url: "/message/{messageId:.*}",
                    templateUrl: "/app/messages-viewmode-details.html"
                });

        }])


    .run(['$rootScope', 'AnalyticsService',
        function($rootScope, AnalyticsService) {

            if ($rootScope.timeZone) {
                moment.tz.setDefault($rootScope.timeZone);
            }

            // Configure Google Analytics
            if (AnalyticsService.enabled()) {

                // initialise google analytics
                AnalyticsService.initAnalytics();

                // track pageview on state change
                $rootScope.$on('$stateChangeSuccess', function () {
                    AnalyticsService.logPageView();
                });
            }
        }]);


/**
 * The view mode bar and filter bar are always visible, but the filter bar can
 * have varying height and may change height when the window is re-sized.
 * Compute the correct top position of the message lists
 */
$( window ).resize(function() {
    adjustMessageListTopPosition();
});

function adjustMessageListTopPosition() {
    var filterBar = $('.filter-bars');
    if (filterBar.length) {
        var offset = 40 + filterBar.height();
        var messageDetails = $(".message-details-list");
        if (messageDetails.length) {
            messageDetails.css("margin-top", offset + "px");
        }
        var messageMap = $(".message-list-map");
        if (messageMap.length) {
            messageMap.css("top", offset + "px");
        }
    }
}
