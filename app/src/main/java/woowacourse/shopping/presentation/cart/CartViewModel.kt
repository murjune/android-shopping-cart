package woowacourse.shopping.presentation.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import woowacourse.shopping.domain.repository.CartRepository
import woowacourse.shopping.presentation.base.BaseViewModelFactory
import woowacourse.shopping.presentation.util.MutableSingleLiveData
import woowacourse.shopping.presentation.util.SingleLiveData

class CartViewModel(
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val _products = MutableLiveData<List<CartProductUi>>(emptyList())
    val products: LiveData<List<CartProductUi>> get() = _products
    private val _currentPage = MutableLiveData(START_PAGE)
    val currentPage: LiveData<Int> get() = _currentPage
    val canLoadPrevPage: LiveData<Boolean>
        get() =
            MediatorLiveData<Boolean>().apply {
                addSource(currentPage) {
                    val prevPage = it - INCREMENT_AMOUNT
                    value = canLoadMoreCartProducts(prevPage)
                }
                addSource(products) {
                    val prevPage = currentPage.value?.minus(INCREMENT_AMOUNT) ?: return@addSource
                    value = canLoadMoreCartProducts(prevPage)
                }
            }
    val canLoadNextPage: LiveData<Boolean>
        get() =
            MediatorLiveData<Boolean>().apply {
                addSource(currentPage) {
                    val nextPage = it + INCREMENT_AMOUNT
                    value = canLoadMoreCartProducts(nextPage)
                }
                addSource(products) {
                    val nextPage = currentPage.value?.plus(INCREMENT_AMOUNT) ?: return@addSource
                    value = canLoadMoreCartProducts(nextPage)
                }
            }
    private val _errorEvent = MutableSingleLiveData<CartErrorEvent>()
    val errorEvent: SingleLiveData<CartErrorEvent> = _errorEvent

    init {
        loadCartProducts(START_PAGE)
    }

    private fun loadCartProducts(page: Int) {
        cartRepository.cartProducts(page, PAGE_SIZE).onSuccess { carts ->
            _products.value = carts.map { it.toUiModel() }
        }.onFailure {
            _errorEvent.setValue(CartErrorEvent.LoadCartProducts)
        }
    }

    private fun canLoadMoreCartProducts(page: Int): Boolean {
        cartRepository.canLoadMoreCartProducts(page, PAGE_SIZE).onSuccess {
            return it
        }.onFailure {
            _errorEvent.setValue(CartErrorEvent.CanLoadMoreCartProducts)
        }
        return false
    }

    fun increaseCartProduct(productId: Long) {
        val currentProducts = _products.value ?: return
        val product = currentProducts.find { it.product.id == productId } ?: return
        val newProduct = product.copy(count = product.count + 1)
        cartRepository.updateCartProduct(productId, newProduct.count).onSuccess {
            val updatedProducts =
                currentProducts.map {
                    if (it.product.id == productId) {
                        newProduct
                    } else {
                        it
                    }
                }
            _products.value = updatedProducts
        }.onFailure {
            _errorEvent.setValue(CartErrorEvent.UpdateCartProducts)
        }
    }

    fun decreaseCartProduct(productId: Long) {
        val currentProducts = _products.value ?: return
        val product = currentProducts.find { it.product.id == productId } ?: return
        val newProduct = product.copy(count = product.count - 1)
        if (newProduct.count <= 0) return _errorEvent.setValue(CartErrorEvent.DecreaseCartCountLimit)
        cartRepository.updateCartProduct(productId, newProduct.count).onSuccess {
            val updatedProducts =
                currentProducts.map {
                    if (it.product.id == productId) {
                        newProduct
                    } else {
                        it
                    }
                }
            _products.value = updatedProducts
        }.onFailure {
            _errorEvent.setValue(CartErrorEvent.UpdateCartProducts)
        }
    }

    fun deleteProduct(product: CartProductUi) {
        cartRepository.deleteCartProduct(product.product.id).onSuccess {
            _products.value = _products.value?.minus(product)
            val currentPage = currentPage.value ?: return
            loadCartProducts(currentPage)
        }.onFailure {
            _errorEvent.setValue(CartErrorEvent.DeleteCartProduct)
        }
    }

    fun plusPage() {
        val currentPage = _currentPage.value ?: return
        loadCartProducts(currentPage + INCREMENT_AMOUNT)
        _currentPage.value = _currentPage.value?.plus(INCREMENT_AMOUNT)
    }

    fun minusPage() {
        val currentPage = _currentPage.value ?: return
        loadCartProducts(currentPage - INCREMENT_AMOUNT)
        _currentPage.value = _currentPage.value?.minus(INCREMENT_AMOUNT)
    }

    companion object {
        private const val START_PAGE: Int = 1
        private const val PAGE_SIZE = 5
        private const val INCREMENT_AMOUNT = 1

        fun factory(repository: CartRepository): ViewModelProvider.Factory {
            return BaseViewModelFactory { CartViewModel(repository) }
        }
    }
}
