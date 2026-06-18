export const useWallets = () => {
    const config = useRuntimeConfig()
    const apiBase = 'http://localhost:8080/v1/api/wallet'

    const fetchWallets = async () => {
        try {
            return await $fetch<any[]>(apiBase)
        } catch (error) {
            console.error('Erro ao carregar carteiras:', error)
            throw new Error('Não foi possível carregar as carteiras.')
        }
    }

    const fetchWalletById = async (id: string) => {
        return await $fetch<any>(`${apiBase}/${id}`)
    }

    const createWallet = async (walletData: any) => {
        return await $fetch(apiBase, {
            method: 'POST',
            body: walletData
        })
    }

    const depositMoney = async (id: string, amount: number) => {
        return await $fetch(`${apiBase}/deposit/${id}`, {
            method: 'POST',
            body: { amount }
        })
    }

    const withdrawMoney = async (id: string, amount: number) => {
        return await $fetch(`${apiBase}/withdraw/${id}`, {
            method: 'POST',
            body: { amount }
        })
    }

    return {
        fetchWallets,
        fetchWalletById,
        createWallet,
        depositMoney,
        withdrawMoney
    }
}