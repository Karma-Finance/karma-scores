# Karma Custom Treasury Documentation

The Karma Custom Treasury handles payment of a payout token against a deposit token.
Only Karma Bond contracts are allowed to deposit funds and receive the payout token.

---

## `KarmaCustomTreasury::constructor`

### 📜 Method Call

- Default contract constructor. In production, it should be deployed by the KarmaFactory.
- Access: Everyone

```java
public KarmaCustomTreasury (
    Address payoutToken, 
    Address initialOwner
)
```

- `payoutToken`: The payout token address
- `initialOwner`: The initial owner of the Custom Treasury

### 🧪 Example call

```java
{
  "payoutToken": "cx000000000000000000000000000000000000000a",
  "initialOwner": "hx0000000000000000000000000000000000000001"
}
```


## `KarmaCustomTreasury::deposit`

### 📜 Method Call

- Deposit an arbitrary amount of `depositToken` and get paid back with an arbitrary amount of `payoutToken`.
- Access: A whitelisted Bond Contract

```java
// @External - this method is external through tokenFallback
private void deposit (
  Address caller,
  Address principleTokenAddress, 
  BigInteger amountPrincipleToken,
  BigInteger amountPayoutToken
)
```

- `caller`: The method caller. This field is handled by tokenFallback
- `principleTokenAddress`: The principal token address. This field is handled by tokenFallback
- `amountPrincipleToken`: The principal token amount sent. This field is handled by tokenFallback
- `amountPayoutToken`: The amount of payout token to be sent back to the bond contract

### 🧪 Example call

```java
{
  "to": principleTokenAddress,
  "method": "transfer",
  "params": {
    "_to": KarmaCustomTreasury,
    "_value": "0xde0b6b3a7640000", // 10**18 - equivalent to amountPrincipleToken
    "_data": hex({
      "method": "deposit",
      "params": {
        "amountPayoutToken": "0x1158e460913d00000" // 20 * 10**18
      }
    })
  },
}
```
