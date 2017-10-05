/**
 * The main Niord Proxy teaser app module definition.
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

    .config(['$translateProvider',
        function ($translateProvider) {
            'use strict';
            $translateProvider.useSanitizeValueStrategy('sanitize');
        }])


    .run(['$rootScope', '$window', '$location',
        function($rootScope, $window, $location) {

            // Configure Google Analytics
            if ($rootScope.analyticsTrackingId && $rootScope.analyticsTrackingId.length > 0) {

                // initialise google analytics
                try {
                    $window.ga('create', $rootScope.analyticsTrackingId, 'auto');
                    $window.ga('send', 'pageview', $location.path());
                } catch (ex) {
                }
            }

        }]);


function adjustMessageListTopPosition() {
}
