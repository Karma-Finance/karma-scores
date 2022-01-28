# Karma Factory Storage Documentation

The Karma Factory Storage

---

## `KarmaFactoryStorage::constructor`

### 📜 Method Call

- Default contract constructor.
- Access: Everyone
- By default, the policy owner is the deployer of the contract

```java
public KarmaFactoryStorage ()
```

## `KarmaFactoryStorage::pushBond`

### 📜 Method Call

- Pushes bond details to array
- Access: Karma Factory

```java
@External
public TreasuryBond pushBond (
  BondDetails bond
)
```

- `bond`: The bond details, encoded as a [BondDetails](#karmafactorystoragebonddetails).

### 🧪 Example call

```java
{
  "to": KarmaFactoryStorage,
  "method": "pushBond",
  "params": {
    "bond": {
      "payoutToken": payoutToken,
      "principleToken": principleToken,
      "customTreasury": customTreasury,
      "customBond": customBond,
      "initialOwner": alice,
      "tierCeilings": [
        "0x3635c9adc5dea00000", 
        "0x6c6b935b8bbd400000", 
        "0xa2a15d09519be00000"
      ], // [1000 * 10**18, 2000 * 10**18, 3000 * 10**18]
      "fees": [
        "0x2710", 
        "0x4e20", 
        "0x7530"
      ] // [10000, 20000, 30000], 1%, 2%, 3%
    }
  }
}
```

## `KarmaFactoryStorage::BondDetails`

### ⚙️ Structure definition

```java
class BondDetails {
  Address payoutToken;
  Address principleToken;
  Address treasuryAddress;
  Address bondAddress;
  Address initialOwner;
  BigInteger[] tierCeilings;
  BigInteger[] fees;
}
```
- `payoutToken`: The payout token address associated with the bond
- `principleToken`: The principal token address associated with the bond
- `customTreasury`: The custom treasury associated with the bond
- `customBond`: The custom bond address
- `initialOwner`: The initial owner of the bond
- `tierCeilings`: Array of ceilings of principal bonded till next tier
- `fees`: Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)


## `KarmaFactoryStorage::setFactoryAddress`

### 📜 Method Call

- Changes the Karma Factory address
- Access: Policy

```java
@External
public void setFactoryAddress (
  Address factory
)
```

- `factory`: The new factory address

### 🧪 Example call

```java
{
  "to": KarmaFactoryStorage,
  "method": "setFactoryAddress",
  "params": {
    "factory": factory
  }
}
```

# Public variable getters

## `KarmaFactoryStorage::bondDetails`

### 📜 Method Call

- Get the details of a bond indexed in the bond array
- Returns a [BondDetails](#karmafactorystoragebonddetails)

```java
@External(readonly = true)
public BondDetails bondDetails (int index)
```

### 🧪 Example call

```java
{
  "to": KarmaFactoryStorage,
  "method": "bondDetails",
  "params": {
    "index": "0x0"
  }
}
```

Result:
```java
{
  "payoutToken": payoutToken,
  "principleToken": principleToken,
  "customTreasury": customTreasury,
  "customBond": customBond,
  "initialOwner": alice,
  "tierCeilings": [
    "0x3635c9adc5dea00000", 
    "0x6c6b935b8bbd400000", 
    "0xa2a15d09519be00000"
  ], // [1000 * 10**18, 2000 * 10**18, 3000 * 10**18]
  "fees": [
    "0x2710", 
    "0x4e20", 
    "0x7530"
  ] // [10000, 20000, 30000], 1%, 2%, 3%
}
```

## `KarmaFactoryStorage::karmaFactory`

### 📜 Method Call

- Get the factory address

```java
@External(readonly = true)
public Address karmaFactory()
```

### 🧪 Example call

```java
{
  "to": KarmaFactoryStorage,
  "method": "karmaFactory"
}
```

Result:
```java
hx123457890123457890123457890123457890
```


## `KarmaFactoryStorage::indexOfBond`

### 📜 Method Call

- Get the index of the bond within the bond array
- May be useful for [KarmaFactoryStorage::bondDetails](#karmafactorystoragebonddetails)

```java
@External(readonly = true)
public int indexOfBond (Address bond)
```

### 🧪 Example call

```java
{
  "to": KarmaFactoryStorage,
  "method": "karmaFactory",
  "bond": bondAddress
}
```

Result:
```java
0x3
```

