# Karma Custom Bond Documentation


## Initialization methods
---

## `KarmaCustomBond::constructor`

### 📜 Method Call

- Default contract constructor.
- Access: Everyone

```java
public KarmaCustomBond (
  Address customTreasury,
  Address payoutToken,
  Address principalToken,
  Address karmaTreasury,
  Address subsidyRouter,
  Address initialOwner,
  Address karmaDAO,
  BigInteger[] tierCeilings,
  BigInteger[] fees
)
```

- `customTreasury`: The custom treasury associated with the bond
- `payoutToken`: The payout token address associated with the bond, token paid for principal
- `principalToken`: The inflow token
- `karmaTreasury`: The Karma treasury
- `subsidyRouter`: pays subsidy in Karma to custom treasury
- `initialOwner`: The initial policy role address
- `karmaDAO`: The KarmaDAO contract address
- `tierCeilings`: Array of ceilings of principal bonded till next tier
- `fees`: Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)

---

## `KarmaCustomBond::initializeBond`

### 📜 Method Call

- Initializes bond parameters
- Access: Policy
- Once the Custom Bond contract is initialized, users can start purchasing bonds

```java
@External
public void initializeBond (
  BigInteger controlVariable,
  Long vestingTerm, // in blocks
  BigInteger minimumPrice,
  BigInteger maxPayout,
  BigInteger maxDebt,
  BigInteger initialDebt
)
```

- `controlVariable`: The Bond Control Variable (BCV). Controls bond price and capacity. BCV directly affects the bond price - the higher the BCV, the higher the bond price. As a higher bond price makes bonds less attractive, the protocol can adjust this value to tune the bond capacity.
- `vestingTerm`: Vesting term (typically 7 days), expressed in ICON blocks. A bond vests linearly to the bonder over a length of time, called the bond vesting term. This means the bonder can claim a portion of the reward tokens each day, with all rewards being claimable at the end of the term.
- `minimumPrice`: Minimum price of the bond
- `maxPayout`: Max payout as a % of total supply
- `maxDebt`: Ceiling on how many bonds can be outstanding
- `initialDebt`: Initial debt used for initializing the contract

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "initializeBond",
  "params": {
    "controlVariable": "0x61a80", // 400000
    "vestingTerm": "0x49d40", // 302400, 1 week (7*24*3600/2) considering 2 seconds blocks
    "minimumPrice": "0x151b", // 5403
    "maxPayout": "1",
    "maxDebt": "0x12a05f200", // 5000000000
    "initialDebt": "0x5cfbb600" // 1560000000
  }
}
```

## Policy Functions

---

## `KarmaCustomBond::setBondTerms`

### 📜 Method Call

- Change the parameters of a bond
- Access: Policy

```java
@External
public void setBondTerms (
    int parameter,
    BigInteger input
)
```

- `parameter`: The input type, its value is either 0 (VESTING, set the `vestingTerm` blocks value), 1 (PAYOUT, set the `maxPayout` value) or 2 (DEBT, set the `maxDebt` value)
- `input`: The input value

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "setBondTerms",
  "params": {
    "parameter": "0x0", // VESTING, set the vesting block
    "input": "0x93a80", // 604800, 2 weeks
  }
}
```

---

## `KarmaCustomBond::setAdjustment`

### 📜 Method Call

- Set control variable adjustment
- Access: Policy

```java
@External
public void setAdjustment (
  boolean addition,
  BigInteger increment,
  BigInteger target,
  Long buffer
)
```

- `addition`: Addition (true) or subtraction (false) of BCV
- `increment`: The increment value of the `controlVariable` value
- `target`: BCV when adjustment finished
- `buffer`: Minimum length (in blocks) between adjustments

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "setAdjustment",
  "params": {
    "addition": "0x1", // true, add
    "increment": "0x1", // increment BCV+1
    "target": "0x6", // BCV+6
    "buffer": "0x1" // 1 block
  }
}
```

## Custom Bond settings

## `KarmaCustomBond::changeKarmaTreasury`

### 📜 Method Call

- Change the Karma Treasury address
- Access: KarmaDAO

```java
@External
public void changeKarmaTreasury (
  Address karmaTreasury
)
```

- `karmaTreasury`: The new Karma Treasury address

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "changeKarmaTreasury",
  "params": {
    "addition": "0x1", // true, add
    "increment": "0x1", // increment BCV+1
    "target": "0x6", // BCV+6
    "buffer": "0x1" // 1 block
  }
}
```

---

## `KarmaCustomBond::paySubsidy`

### 📜 Method Call

- Subsidy controller checks payouts since last subsidy and resets counter
- Access: Subsidy Controller

```java
@External
public BigInteger paySubsidy()
```

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "paySubsidy"
}
```

## User functions


## `KarmaCustomBond::deposit`

### 📜 Method Call

- Deposit bond
- Access: Subsidy Controller

```java
// @External - this method is external through tokenFallback
private void deposit (
  Address caller, 
  Address token, 
  BigInteger amount, 
  BigInteger maxPrice,
  Address depositor
)
```

- `caller`: The method caller. This field is handled by tokenFallback
- `token`: Only principalToken is accepted. This field is handled by tokenFallback
- `amount`: Amount of principal inflow token received. This field is handled by tokenFallback
- `maxPrice`: Max price for slippage protection. `maxPrice` value needs to be superior or equal to the bond price, otherwise the transaction will fail.
- `depositor`: Registered depositor of the bond

### 🧪 Example call

```java
{
  "to": principalTokenAddress,
  "method": "transfer",
  "params": {
    "_to": KarmaCustomBond,
    "_value": "0xde0b6b3a7640000", // 10**18 - equivalent to amount parameter
    "_data": hex({
      "method": "deposit",
      "params": {
        "maxPrice": "0x1158e460913d00000" // 20 * 10**18
        "depositor": alice
      }
    })
  }
}
```

---

## `KarmaCustomBond::redeem`

### 📜 Method Call

- Redeem bond for user
- Access: Everyone
- Returns the payout amount

```java
@External
public BigInteger redeem (
    Address depositor
)
```

- `depositor`: Depositor address, it will also be used as a destination address for the redeemed tokens

### 🧪 Example call

```java
{
  "to": KarmaCustomBond,
  "method": "redeem",
  "params": {
    "addition": "0x1", // true, add
    "increment": "0x1", // increment BCV+1
    "target": "0x6", // BCV+6
    "buffer": "0x1" // 1 block
  }
}
```
