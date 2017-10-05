
/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Common settings.
 */
angular.module('niord.proxy.conf')

    .run(['$rootScope', function ($rootScope) {

        /** SETTINGS START **/
                
        /**
         * Everything between the SETTINGS START and END comments is substituted 
         * with database settings by the SiteConfigServlet.
         */

        $rootScope.languages = [ "en" ];
        $rootScope.analyticsTrackingId = "";
        $rootScope.executionMode = "DEVELOPMENT";
        $rootScope.wmsLayer = false;

        /** SETTINGS END **/

    }]);
