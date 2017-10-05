
/**
 * Translations.
 */
angular.module('niord.proxy.app')

    .config(['$translateProvider', function ($translateProvider) {

        $translateProvider.translations('en', {

            'TERM_APPLY' : 'Apply',
            'TERM_CANCEL' : 'Cancel',
            'MENU_DETAILS' : 'Details',
            'MENU_MAP' : 'Map',
            'MENU_TABLE' : 'List',
            'MENU_PUBLICATIONS' : 'Publications',
            'MENU_PRINT' : 'Print',
            'MENU_NW'  : 'Navigational Warnings',
            'MENU_NM'  : 'Notices to Mariners',
            'MENU_DOWNLOADS'  : 'Downloads',
            'BTN_CLOSE' : 'Close',
            'GENERAL_MSGS' : 'General Messages',
            'NO_POS_MSGS' : 'Additional Messages',
            'MINIMIZE' : 'Minimize',
            'MAXIMIZE' : 'Maximize',
            'FIELD_REFERENCES' : 'References',
            'FIELD_ATTACHMENTS' : 'Attachments',
            'FIELD_CHARTS' : 'Charts',
            'FIELD_PUBLICATION' : 'Publication',
            'FIELD_PUBLISHED' : 'Published',
            'PART_TYPE_DETAILS' : 'Details',
            'PART_TYPE_TIME' : 'Time',
            'PART_TYPE_POSITIONS' : 'Positions',
            'PART_TYPE_NOTE' : 'Note',
            'PART_TYPE_PROHIBITION' : 'Prohibition',
            'PART_TYPE_SIGNALS' : 'Signals',
            'MAIN_TYPE_NW' : 'NW',
            'MAIN_TYPE_NM' : 'NM',
            'TYPE_TEMPORARY_NOTICE' : 'Temp.',
            'TYPE_PRELIMINARY_NOTICE' : 'Prelim.',
            'TYPE_PERMANENT_NOTICE' : 'Perm.',
            'TYPE_MISCELLANEOUS_NOTICE' : 'Misc.',
            'TYPE_LOCAL_WARNING' : 'Local',
            'TYPE_COASTAL_WARNING' : 'Coastal',
            'TYPE_SUBAREA_WARNING' : 'Subarea',
            'TYPE_NAVAREA_WARNING' : 'Navarea',
            'STATUS_CANCELLED' : 'Cancelled',
            'STATUS_EXPIRED' : 'Expired',
            'REF_REPETITION' : '(repetition)',
            'REF_REPETITION_NEW_TIME' : '(repetition with new time)',
            'REF_CANCELLATION' : '(cancelled)',
            'REF_UPDATE' : '(updated repetition)',
            'SOURCE_DATE_FORMAT' : 'D MMMM YYYY',
            'LAYER_WMS': 'Sea Chart',
            'LAYER_LABELS': 'Labels',
            'ACTIVE_NOW' : 'Show Active Now',
            'ACTIVE_DATE' : 'Select Date Interval',
            'DATE_FROM' : 'From',
            'DATE_TO' : 'To',
            'CUSTOM_DATE_RANGE' : 'Date Interval',
            'SHOW_ATTACHMENTS' : 'Show attachments',
            'HIDE_ATTACHMENTS' : 'Hide attachments',
            'MSG_NOT_FOUND' : 'Message {{messageId}} is not available',
            'MORE_MSGS' : ' and {{messageNo}} more messages',
            'TEASER_TEXT': 'Click the map for<br>Notices to Mariners<br>and Navigational Warnings',
            'MAP_COPYRIGHT' : '&copy; <a href="http://www.ghanamaritime.org" target="_blank">Ghana Maritime Authority</a>.',
            'FOOTER_COPYRIGHT' : '&copy; 2017 Ghana Maritime Authority',
            'FOOTER_DISCLAIMER' : 'Disclaimer',
            'FOOTER_COOKIES' : 'Cookies'
        });

        $translateProvider.preferredLanguage('en');

    }]);

