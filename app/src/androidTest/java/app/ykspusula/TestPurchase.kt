package app.ykspusula

import app.ykspusula.monetize.PurchaseQuery
import app.ykspusula.monetize.REMOVE_ADS_PRODUCT
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * Debug derlemesindeki test anahtarının özel yarısı. Yalnızca otomatik testlerde satın alma kaydı imzalamak içindir.
 * Google Play anahtarı değildir; Play derlemesi bu anahtarı kabul etmez.
 */
object TestPurchase {
    private const val PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC0PPLLH7NsCVhku+FiOJdeEP2VEtxuV9HauJB8HbjuBsXq1PzcEPaWwHK72K3zfw5VQ8Z7OyMnp+6JTQ9fWd4hebzHW+if7zeiVBkWIrzKCy02MVHQXGQ+DLpfq/8s8WVEgDtrRHluJA2j2yNp4NxbDoLetGzrF1MiLCiOKqK4U7WM3G2TIXj2O1Qtg4Zzq1LYn1diaKLHwmlAszA00Vn9+HQnJrepK69aIOi5E57SKu9XLpVL53NCnxbfiXZEdsZgEICRbOetxxB0rIlLKjYytxTAq2slWUmodsRaKw6isl7E8reV/lMcBn+HE5hJ3B9ZRtiA4gkiTKpk5LAeA8NXAgMBAAECggEAP8SslicyINAxoPpEJK8nBtX2Eg8pRDT3/217WR5e9oAY+BvBFs89n1qZfVKNQPA8Dvi6uZDTwE0O/S7xeGfgSnG608H1zNgjE+CzO3yw1L8dkOXkL1bw9G8mZYEB+FGz5YeE/k+sssY4uTdoESbIb8LzlH27Z3XKKPX8p0f4aEREtaZAaMEd+7GdZoibqEfIWd/EPsR0uUdWZI1ip98muhISUz9FwrpkTjIAOnJ6s3RYb2MyKheDOsZtP9kFyjnL6vIb15zuJV+LmpU0nh5TfYs/5qqTNJuDUFE8NAR2Rb+J99iqOrjJb0HjJW9gls4UkzDjvj98uKljm9tz/l+YkQKBgQDvw/CZPGtMzypzaWJ80LQhfyzJRBrFwHRfMIaCEZUEtRZXkb6fHuapGWwA6dkKN4G/y/ssvVlsYhU4dnDK/uRVuYB3ViBMqVlFofeqLFnkeCl4VEk0WoOTi5WUI/zz2MHYJbKlotq2J+lWGmlfMBx+HRb7iZ7iCKD0OP/4LIckvwKBgQDAcSufLYYrqGlsUYyENhmIh8MEqszK5ELWLy3ezVSSZHFndTo+boL+zgvB/YDlmWbX8zU/AfSXVH3P5mp+1JB2UT10RiTI1S2fyP4rGULLES5eipXuEL9YBq6M0WeRcMnJi93tw+VaYLp8pT4Z/Gf2dH0/eB2Mbg2pVBSuS7GPaQKBgH4ceqWu8OVCphp0WNg70jM4OC9Ywhg3pT4YQPeTFeWW9jxzLWd2km5pL7S/S2ijT3y5ZtDee3WVlYHQUn2DWkh4vMiIJY8NGCaXDbSVhWa1iWKAOsztuv1VUwTLdRYxcawmD/QtV4quLX4bgEcQADyZQmhlt4s+bKJohMJdcWvzAoGBALOEHowjfGiXdph7zhe7G8B33J0q81VE73A7nI+dfQn+tqcq/MUmc/5bmS5CW45StlQ2ZPI80bylLaPVrnxXr9cfW7Nuh3vtGN1YMnlj3CHwChBZORlV+BiWSxfDc9iBnUrstX7J4daYKv6Batf008pcykV9uBq6wvW1IIGizKUZAoGBAMpCRYOMq6oGNFKYp1vQAsdUeyPELGOyyjE2AWVQO0xLvT5goHBVDWs8PObC7UnWUBkDFQPxVRKhmjbfIkQHHi7h4eIwzRbFiSBGLpaghxjMyYFu0bOhlc832PFa3OVXa7hd3GV29E1eCEWGrVSf4ZM5ePtLws9sMAwW+Crs51Yf"

    fun sign(json: String): String {
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY)))
        return Signature.getInstance("SHA1withRSA").run { initSign(key); update(json.toByteArray()); Base64.getEncoder().encodeToString(sign()) }
    }

    fun json(product: String = REMOVE_ADS_PRODUCT, state: Int = 0, token: String = "tok-$product") =
        "{\"orderId\":\"GPA.test\",\"packageName\":\"app.ykspusula\",\"productId\":\"$product\",\"purchaseState\":$state,\"purchaseToken\":\"$token\"}"

    /** BillingClient'tan gelen belirteç JSON'dakiyle aynıdır; tokenOverride farklı bir belirteç dener. */
    fun purchased(product: String = REMOVE_ADS_PRODUCT, state: Int = 0, tokenOverride: String? = null): PurchaseQuery.Purchased {
        val j = json(product, state)
        return PurchaseQuery.Purchased(tokenOverride ?: "tok-$product", j, sign(j))
    }
}
