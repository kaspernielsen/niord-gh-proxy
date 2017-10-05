/**
 * The main Niord Proxy app filters.
 */

angular.module('niord.proxy.app')

    /********************************
     * Renders the message details
     ********************************/
    .filter('toTrusted', ['$sce', function ($sce) {
        return function (value) {
            return $sce.trustAsHtml(value);
        };
    }]);
