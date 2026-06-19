package com.paytrail.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the webhook sample embedded in docs/paytrail-api.postman_collection.json.
 *
 * The collection ships a pre-signed charge.success payload so that developers can
 * immediately test the webhook endpoint without computing their own signature. This
 * test locks the documented body and signature as constants so that any drift —
 * a change to HmacUtil, a typo in the collection — is caught immediately.
 *
 * To regenerate: update BODY and SIGNATURE together, keeping them in sync with
 * the body and x-paystack-signature values in the Postman collection.
 */
class PostmanWebhookSampleTest {

    static final String BODY =
            "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref_demo_1\",\"amount\":500000," +
            "\"currency\":\"NGN\",\"channel\":\"card\",\"customer\":{\"email\":\"demo@paytrail.test\"," +
            "\"first_name\":\"Demo\",\"last_name\":\"User\"},\"metadata\":{\"merchant_id\":\"demo-merchant\"}}}";

    static final String SECRET = "test_secret_key_for_demo";

    static final String SIGNATURE =
            "99b4502b889eb0ebc2f0eaf9c823e4a20a55b52decff687352ed7acb46aab7f0" +
            "9bfbf95c70ecde8148fba82df395376a1904fd329e64de0abd6c675b1200c936";

    @Test
    void postmanWebhookSampleSignatureIsValid() {
        assertTrue(
                HmacUtil.verifySignature(BODY, SECRET, SIGNATURE),
                "The HMAC-SHA512 signature in the Postman collection is no longer valid. " +
                "Update BODY and SIGNATURE constants in this test to match the collection."
        );
    }
}
