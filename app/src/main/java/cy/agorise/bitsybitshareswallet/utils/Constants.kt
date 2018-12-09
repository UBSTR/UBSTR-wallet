package cy.agorise.bitsybitshareswallet.utils

import cy.agorise.graphenej.Asset


object Constants {

    /** Key used to store the number of the last agreed License version */
    const val KEY_LAST_AGREED_LICENSE_VERSION = "key_last_agreed_license_version"

    /** Version of the currently used license */
    const val CURRENT_LICENSE_VERSION = 1

    /** Key used to store if the assets database has been loaded or not */
    const val KEY_DATABASE_LOADED = "key_database_loaded"

    /**
     * Key used to store a preference value used to keep track of the last time the assets in
     * database were updated.
     */
    const val KEY_LAST_ASSET_LIST_UPDATE = "key_last_assets_update"

    /** Key used to store if the initial setup is already done or not */
    const val KEY_INITIAL_SETUP_DONE = "key_initial_setup_done"

    /** Key used to store the id value of the currently active account in the shared preferences */
    const val KEY_CURRENT_ACCOUNT_ID = "key_current_account_id"

    /** The minimum required length for a PIN number */
    const val MIN_PIN_LENGTH = 6

    /** The user selected encrypted PIN */
    const val KEY_ENCRYPTED_PIN = "key_encrypted_pin"

    /**
     * LTM accounts come with an expiration date expressed as this string.
     * This is used to recognize such accounts from regular ones.
     */
    const val LIFETIME_EXPIRATION_DATE = "1969-12-31T23:59:59"

    /**
     * Time period between two consecutive requests to the full node performed whenever we have
     * open payment requests as a matter of redundancy.
     */
    const val MISSING_PAYMENT_CHECK_PERIOD: Long = 5000

    /**
     * Time period to wait to send a request to the NetworkService, and retry in case it is still not connected
     */
    const val NETWORK_SERVICE_RETRY_PERIOD: Long = 5000

    /** Key used to store the night mode setting into the shared preferences */
    const val KEY_NIGHT_MODE_ACTIVATED = "key_night_mode_activated"
}
