<p align="center">
  <img 
    src="https://i.imgur.com/BHTdL4Q.png" 
    alt="KarmaDAO logo">
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Karma Bond SCOREs

## Requirements

You need to install JDK 11 or later version. Visit [OpenJDK.net](http://openjdk.java.net/) for prebuilt binaries.
Or you can install a proper OpenJDK package from your OS vendors.
You will also need `jq`.

Please find below the recommanded commands to run to install these packages:

- In macOS:

```bash
$ # Install brew
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
$ # Install JDK
$ brew tap AdoptOpenJDK/openjdk
$ brew cask install adoptopenjdk11
$ # Install JQ
$ brew install jq
```

- In Windows (Windows 10 or later)

You can install the JDK11 natively, but you will need `bash` in order to run the Karma deploy scripts.
Please [install WSL](https://docs.microsoft.com/en-us/windows/wsl/install-manual) first, so you can follow the same instructions than Linux.

- In Linux (Ubuntu 18.04):

```bash
$ # Install JDK
$ sudo apt install openjdk-11-jdk
$ # Install JQ
$ sudo apt install jq
```


## How to Build and Deploy the project

### 1. Build the project (mostly for making sure that everything works correctly for your setup)

$ ./gradlew build

This should run all the unittest, and run successfully with a similar output than below:

```java
Starting a Gradle Daemon (subsequent builds will be faster)

> Task :Karma-Bond:Tests:Karma-CustomBond-Test:test
> Task :Karma-Bond:Tests:Karma-CustomTreasury-Test:test
> Task :Karma-Bond:Tests:Karma-Factory-Test:test
> Task :Karma-Bond:Tests:Karma-FactoryStorage-Test:test
> Task :Karma-Bond:Tests:Karma-SubsidyRouter-Test:test

BUILD SUCCESSFUL in 3m 53s
104 actionable tasks: 104 executed
```

### 2. Setup the Karma build system

You'll need python3 and pip installed on your machine beforehand.

Now we can install the virtualenv  and the dependancies:

```bash
$ # Run this in the root folder of the project
$ python -m venv ./venv
$ source ./venv/bin/activate
$ pip install -r ./requirements.txt
```

Everytime you want to use the Karma build & deploy system, please do `source ./venv/bin/activate` beforehand.

### 3. Deploy the Karma Bond core contracts

Karma Bond SCOREs contracts are mainly composed of two parts: the core that includes factories and DAO parts, and the bonds contracts.
We start by deploying the factories and the DAO contracts first.

```bash
$ # Run this in the root folder of the project
$ # We specify "sejong" as an argument here, which means the contracts
$ # will be deployed on Sejong. You can set it to "custom" too for the
$ # custom Karma network
$ ./scripts/scenario/1.deploy_karmabond_core.sh sejong
[...]
# This should end with the following message
[🎉] All Karma Bond core contracts have been successfully deployed!
```

### 4. Deploy the Karma Custom Treasury contract

In order to deploy a bond, we need to deploy its associated custom treasury contract first.

For doing so, we will need to create a new configuration file, that will both contain information about the treasury and the bond.

These configuration files are located in [`./scripts/scenario/setup/`](./scripts/scenario/setup/).

They will be named like that : `${bondId}.py`. A `bondId` is an arbitrary number that will represents the bond being deployed. This number must be unique for all bonds accross all networks. You can find an example of a config file [here](scripts/scenario/configs/1.py). 

That file contain a [`config`](https://github.com/Protokol7/karma-scores/blob/5ba717ea89e2061f5b7c60e48a311541287f5bfc/scripts/scenario/configs/1.py#L6) dict variable that you can modify depending of your needs.

There are two important variables in that dict: 
  - "network", that will be the network where the bond is deployed. It can be "custom" too.
  - "implementation", that will be either "Base", or "Balanced". 
    - If it is "Base", it will use the generic Bond implementation that uses standard IRC2 tokens for the principal and the payout tokens.
    - If it is "Balanced", it will use the Balanced Bond implementation that uses LP tokens and Balanced poolIDs

For the Custom Treasury, please also look at the [`"treasury"`](https://github.com/Protokol7/karma-scores/blob/5ba717ea89e2061f5b7c60e48a311541287f5bfc/scripts/scenario/configs/1.py#L63) key of the `config` dictionary.
You can change the `initialOwner` and the `initialPayoutFunding` values.

Once you've changed these values, you can deploy the customTreasury by doing so:

```bash
$ # Run this in the root folder of the project
$ # We specify "1" as an argument here, because we've named our config
$ # file "1.py", as we want a Bond ID = 1
$ ./scripts/scenario/2.deploy_customtreasury.sh 1
[...]
# This should end with the following message
[🎉] Karma Custom Treasury contract has been successfully deployed!
```

### 5. Deploy the Karma Custom Bond contract

The process for deploying a custom bond contract is similar to the custom treasury one. If you haven't deployed a custom treasury contract already, please make sure to deploy it correctly first, and come back to these instructions.

We'll need to modify the remaining variables in the [`config`](https://github.com/Protokol7/karma-scores/blob/5ba717ea89e2061f5b7c60e48a311541287f5bfc/scripts/scenario/configs/1.py#L19) dict in the config file, located in the `"bond"` key.

These variables will be needed:

- principalToken: A principal token address
- payoutToken: A payout token address
- initialOwner: Initial owner of the custom bond
- vestingTermSeconds: Vesting term value (in seconds)
- fees: Array of fees tiers, in ten-thousandths (i.e. 33300 = 3.33%)
- tierCeilings: Array of ceilings of principal bonded till next tier
- initialize: please see [`initializeBond`](Karma-Bond/Contracts/Karma-CustomBond/docs/README.md#karmacustombondinitializebond) documentation about these values

Once you've finished setting up these parameters, you can deploy the custom bond using the following command:


```bash
$ # Run this in the root folder of the project
$ # We specify "1" as an argument here, because we've named our config
$ # file "1.py", as we want a Bond ID = 1
$ ./scripts/scenario/3.deploy_custombond.sh 1
[...]
# This should end with the following message
[🎉] Karma Custom Bond contract has been successfully deployed!
```
