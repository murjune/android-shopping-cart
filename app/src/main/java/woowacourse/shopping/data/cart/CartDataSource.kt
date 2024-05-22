package woowacourse.shopping.data.cart

import woowacourse.shopping.domain.entity.CartProduct
import woowacourse.shopping.domain.entity.Product

interface CartDataSource {
    fun loadCartProducts(
        currentPage: Int,
        productSize: Int,
    ): List<CartProduct>

    fun addCartProduct(product: Product): Long

    fun deleteCartProduct(product: Product): Long

    fun canLoadMoreCartProducts(
        currentPage: Int,
        pageSize: Int,
    ): Boolean
}