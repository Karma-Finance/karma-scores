# Karma Custom Treasury for Balanced Documentation

The Karma Custom Treasury handles payment of a payout token against a deposit token.
Only Karma Bond contracts are allowed to deposit funds and receive the payout token. The Treasury owner can whitelist bonds contracts and withdraw tokens from the Treasury.

---

## `KarmaCustomTreasuryBalanced::constructor`

### 📜 Method Call

- Default contract constructor. In production, it should be deployed by the KarmaFactory.
- Access: Everyone

```java
public KarmaCustomTreasuryBalanced (
    Address payoutToken, 
    Address initialOwner,
    BigInteger poolIdPrincipalToken
)
```

- `payoutToken`: The payout token address, token paid for principal
- `initialOwner`: The initial owner of the Custom Treasury
- `poolIdPrincipalToken`: The principal token Balanced Pool ID

### 🧪 Example call

```java
{
  "payoutToken": "cx000000000000000000000000000000000000000a",
  "initialOwner": "hx0000000000000000000000000000000000000001",
  "poolIdPrincipalToken": "0x3"
}
```


## `KarmaCustomTreasuryBalanced::deposit`

### 📜 Method Call

- Deposit an arbitrary amount of `depositToken` and get paid back with an arbitrary amount of `payoutToken`.
- Access: A whitelisted Bond Contract

```java
// @External - this method is external through tokenFallback
private void deposit (
  Address caller,
  Address principalTokenAddress, 
  BigInteger poolIdPrincipalToken,
  BigInteger amountPrincipalToken,
  BigInteger amountPayoutToken
)
```

- `caller`: The method caller. This field is handled by tokenFallback
- `principalTokenAddress`: The principal inflow token address. This field is handled by tokenFallback
- `poolIdPrincipalToken`: The principal token Balanced Pool ID. This field is handled by tokenFallback
- `amountPrincipalToken`: The principal token amount sent. This field is handled by tokenFallback
- `amountPayoutToken`: The amount of payout token to be sent back to the bond contract

### 🧪 Example call

```java
{
  "to": principalTokenAddress,
  "method": "transfer",
  "params": {
    "_to": KarmaCustomTreasuryBalanced,
    "_value": "0xde0b6b3a7640000", // 10**18 - equivalent to amountPrincipalToken
    "_data": hex({
      "method": "deposit",
      "params": {
        "amountPayoutToken": "0x1158e460913d00000" // 20 * 10**18
      }
    })
  }
}
```


## `KarmaCustomTreasuryBalanced::withdraw`

### 📜 Method Call

- Policy can withdraw any IRC2 token to a desired address
- Access: Policy

```java
@External
public void withdraw (
    Address token,
    Address destination,
    BigInteger amount
)
```

- `token`: The token to withdraw
- `destination`: The destination address of the withdrawn tokens
- `amount`: The amount of tokens

### 🧪 Example call

```java
{
  "to": KarmaCustomTreasuryBalanced,
  "method": "withdraw",
  "params": {
    "token": "cx000000000000000000000000000000000000000b",
    "destination": "hx00000000000000000000000000000000000000ff", // any address
    "amount": "0xde0b6b3a7640000", // 10**18
  }
}
```

## `KarmaCustomTreasuryBalanced::toggleBondContract`

### 📜 Method Call

- Toggle a contract in the bond contract whitelist
- Access: Policy

```java
@External
public void toggleBondContract (
  Address bondContract
)
```

- `bondContract` The bond contract to toggle

### 🧪 Example call

```java
{
  "to": KarmaCustomTreasuryBalanced,
  "method": "toggleBondContract",
  "params": {
    "bondContract": "cx000000000000000000000000000000000000000b" // a bond contract
  }
}
```

# Public variable getters

## `KarmaCustomTreasuryBalanced::payoutToken`

### 📜 Method Call

- Return the payout token address

```java
@External(readonly = true)
public Address payoutToken()
```

### 🧪 Example call

```java
{
  "to": KarmaCustomTreasuryBalanced,
  "method": "payoutToken"
}
```

Result:
```java
"cx000000000000000000000000000000000000000a"
```


## `KarmaCustomTreasuryBalanced::bondContract`

### 📜 Method Call

- Return the status of a given address in the bond contract whitelist

```java
@External(readonly = true)
public boolean bondContract (Address address)
```

- `address`: Any address

### 🧪 Example call

```java
{
  "to": KarmaCustomTreasuryBalanced,
  "method": "bondContract",
  "params": {
    "address": "cx000000000000000000000000000000000000000b" // a bond contract previously whitelisted
  }
}
```

Result:
```java
"0x1" // true
```

# View Functions

## `KarmaCustomTreasuryBalanced::valueOfToken`

### 📜 Method Call

- Returns payout token valuation of principal token
- Simply put, it returns `amount` * 10**(`payoutDecimals`) / 10**(`principalDecimals`)

```java
@External(readonly = true)
public BigInteger valueOfToken (
    Address principalTokenAddress,
    BigInteger amount
)
```

- `principalTokenAddress`: The principal token address
- `amount`: An amount of principal token

### 🧪 Example call

```java
{
  "to": KarmaCustomTreasuryBalanced,
  "method": "valueOfToken",
  "params": {
    "principalTokenAddress": "cx000000000000000000000000000000000000000b",
    "amount": "0x1158e460913d00000" // 20 * 10**18
  }
}
```

Result:
```java
"0x1158e460913d00000" // 20 * 10**18
```
