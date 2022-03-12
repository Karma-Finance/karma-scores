<p align="center">
  <img 
    src="https://i.imgur.com/BHTdL4Q.png" 
    alt="KarmaDAO logo">
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Karma SCOREs

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


## How to Run

### 1. Build the project

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

Now we can install the virtualenv (run this in the root folder of the project):

```bash
$ python -m venv ./venv
$ source ./venv/bin/activate
$ pip install -r ./requirements.txt
```

Everytime you want to use the Karma build & deploy system, please do `source ./venv/bin/activate` beforehand.

