package cy.agorise.bitsybitshareswallet.utils


object Constants {

    /** Key used to store the number of the last agreed License version */
    const val KEY_LAST_AGREED_LICENSE_VERSION = "key_last_agreed_license_version"

    /** Version of the currently used license */
    const val CURRENT_LICENSE_VERSION = 1

    /** Key used to store the id value of the currently active account in the shared preferences */
    const val KEY_CURRENT_ACCOUNT_ID = "key_current_account_id"

    /** The minimum required length for a PIN number */
    const val MIN_PIN_LENGTH = 6

    /** Key used to store if the initial setup is already done or not */
    const val KEY_INITIAL_SETUP_DONE = "key_initial_setup_done"

    /** Key used to store the night mode setting into the shared preferences */
    const val KEY_NIGHT_MODE_ACTIVATED = "key_night_mode_activated"
}
