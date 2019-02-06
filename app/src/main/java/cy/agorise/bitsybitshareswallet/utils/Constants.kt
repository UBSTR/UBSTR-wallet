package cy.agorise.bitsybitshareswallet.utils

import cy.agorise.graphenej.UserAccount

object Constants {

    /** Key used to store the number of the last agreed License version */
    const val KEY_LAST_AGREED_LICENSE_VERSION = "key_last_agreed_license_version"

    /** Version of the currently used license */
    const val CURRENT_LICENSE_VERSION = 1

    /** Key used to store the id value of the currently active account in the shared preferences */
    const val KEY_CURRENT_ACCOUNT_ID = "key_current_account_id"

    /** The minimum required length for a PIN number */
    const val MIN_PIN_LENGTH = 6

    /** Name of the account passed to the faucet as the referrer */
    const val FAUCET_REFERRER = "agorise"

    /** Faucet URL used to create new accounts */
    const val FAUCET_URL = "https://faucet.palmpay.io"

    /** Coingecko's API URL */
    const val COINGECKO_URL = "https://api.coingecko.com"

    /** The user selected encrypted PIN */
    const val KEY_ENCRYPTED_PIN = "key_encrypted_pin"

    /** The fee to send in every transfer (0.01%) */
    const val FEE_PERCENTAGE = 0.0001

    /** The account used to send the fees */
    val AGORISE_ACCOUNT = UserAccount("1.2.390320", "agorise")

    /** List of assets symbols that send fee to Agorise when sending a transaction (BTS and smartcoins only) */
    val assetsWhichSendFeeToAgorise = setOf(
        "1.3.0",    // BTS
        "1.3.113",  // CNY
        "1.3.121",  // USD
        "1.3.1325", // RUBLE
        "1.3.120",  // EUR
        "1.3.103"   // BTC
//        "1.3.109",  // HKD
//        "1.3.119",  // JPY
//        "1.3.102",  // KRW
//        "1.3.106",  // GOLD
//        "1.3.105",  // SILVER
//        "1.3.118",  // GBP
//        "1.3.115",  // CAD
//        "1.3.1017", // ARS
//        "1.3.114",  // MXN
//        "1.3.111",  // SEK
//        "1.3.117",  // AUD
//        "1.3.116",  // CHF
//        "1.3.112",  // NZD
//        "1.3.110",  // RUB
//        "1.3.2650", // XCD
//        "1.3.107",  // TRY
//        "1.3.108"   // SGD
    )

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

    /** Time period to wait to send a request to the NetworkService, and retry in case it is still not connected */
    const val NETWORK_SERVICE_RETRY_PERIOD: Long = 5000

    /** Bitshares block period */
    const val BLOCK_PERIOD: Long = 3000

    /**  Key used to store the number of operations that the currently selected account had last time we checked */
    const val KEY_ACCOUNT_OPERATION_COUNT = "key_account_operation_count"

    /** Key used to store the auto close app if no user activity setting into the shared preferences */
    const val KEY_AUTO_CLOSE_ACTIVATED = "key_auto_close_activated"

    /** Key used to store the night mode setting into the shared preferences */
    const val KEY_NIGHT_MODE_ACTIVATED = "key_night_mode_activated"

    const val MERCHANTS_WEBSERVICE_URL = "https://websvc.palmpay.io/"

    /** Key used to store the last time in millis that the merchants info was refreshed */
    const val KEY_MERCHANTS_LAST_UPDATE = "key_merchants_last_update"

    /** Key used to store the last time in millis that the tellers info was refreshed */
    const val KEY_TELLERS_LAST_UPDATE = "key_tellers_last_update"

    /** Constant used to decide whether or not to update the tellers and merchants info from the webservice */
    const val MERCHANTS_UPDATE_PERIOD = 1000L * 60 * 60 + 24 // 1 day

    /** Name of the external storage folder used to save files like PDF and CSV exports and Backups **/
    const val EXTERNAL_STORAGE_FOLDER = "BiTSy"
}
