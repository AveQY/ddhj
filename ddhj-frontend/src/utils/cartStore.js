import { ref, watch, computed } from 'vue'

const cartItems = ref([])

// 初始化从 localStorage 加载
const savedCart = localStorage.getItem('ddhj_cart')
if (savedCart) {
    try {
        cartItems.value = JSON.parse(savedCart)
    } catch (e) {
        console.error('Failed to parse saved cart', e)
    }
}

// 监听变化并同步到 localStorage
watch(cartItems, (newVal) => {
    localStorage.setItem('ddhj_cart', JSON.stringify(newVal))
}, { deep: true })

export const useCartStore = () => {
    const totalCount = computed(() => {
        return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
    })

    const totalPrice = computed(() => {
        return cartItems.value.reduce((sum, item) => {
            const price = parseFloat(item.price) || 0
            const quantity = parseInt(item.quantity) || 0
            return sum + price * quantity
        }, 0)
    })

    const addToCart = (product, spec, quantity = 1, categoryName = '') => {
        const item = {
            productId: product.id,
            productName: product.name,
            image: product.image || (product.images && product.images.length > 0 ? product.images[0] : null),
            specId: spec ? spec.id : null,
            specName: spec ? spec.name : null,
            price: product.sellPrice,
            quantity: quantity,
            categoryName: categoryName
        }

        const existing = cartItems.value.find(i =>
            i.productId === item.productId && i.specId === item.specId
        )

        if (existing) {
            existing.quantity += item.quantity
            if (item.categoryName) {
                existing.categoryName = item.categoryName
            }
        } else {
            cartItems.value.push(item)
        }
    }

    const updateQuantity = (item, quantity) => {
        if (quantity <= 0) {
            const idx = cartItems.value.findIndex(i => i === item)
            if (idx > -1) {
                cartItems.value.splice(idx, 1)
            }
        } else {
            item.quantity = quantity
        }
    }

    const clearCart = () => {
        cartItems.value = []
    }

    return {
        cartItems,
        totalCount,
        totalPrice,
        addToCart,
        updateQuantity,
        clearCart
    }
}
