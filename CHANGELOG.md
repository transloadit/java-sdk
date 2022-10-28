### 0.4.3 / 2022-10-28 ###
* Includes a vulnerability patch in the used socket-io implementation

### 0.4.2 / 2022-02-03 ###
* Added possibility for SDKs using this SDK to send their own version number to the server in the Transloadit-Client header.
* Resolved some file-name conflicts with the tus-java-client library.

### 0.4.1 / 2021-09-26 ###
* Added debugging features regarding HTTP-requests, which should not be used in production without contacting Transloadit support.

### 0.4.0 / 2021-09-26 ###
* Added support for client-side Assembly IDs. You can obtain the ID of an Assembly now before even uploading/saving it. You can achieve this with the brand-new Assembly#getAssemblyID() method.
* Added debugging features regarding AssemblyIDs, which should not be used in production without contacting Transloadit support.
* Also updated the AssemblyListener interface to provide HashMaps instead of JSONObjects.

### 0.3.0 / 2021-06-27 ###
* Updated all dependencies to their most recent, compatible version
  => minimal requirements for the SDK are now Android 5+ and Java 8+.
* Add (form) fields to an Assembly or Template with the addField()- and addFields() - methods
* Extended support for Assembly progress updates via the Websocket.
  => AssemblyListener Interface provides more callback functions now. This should be considered before the update.
* Codebase received a review and an updated JavaDoc
* New Example added that uses [Kotlin](https://kotlinlang.org/).
  
### 0.2.0 / 2021-05-17 ###
* Added retry functionality for assemblies in case of reaching the rate limit

### 0.1.6 / 2021-02-24 ###

* Fix bug that doesn't allow usage of templates that have disabled allow steps override. 
* Added some new examples

### 0.1.5 / 2019-07-16 ###

* Make tus uploads to assembly's tus url
* Make assembly wait till completion

### 0.1.4 / 2019-04-27 ###

* Use a fallback version

### 0.1.3 / 2019-04-18 ###

* load sdk version via ResourceBundle

### 0.1.2 / 2019-04-09 ###

* send client version via "Transloadit-Client" header
* Do not use deprecated status_endpoint property
* update tus-java-client version

### 0.1.1 / 2018-04-23 ###

* Allow configurable upload chunk size [#21](https://github.com/transloadit/java-sdk/issues/21)

### 0.1.0 / 2018-04-05 ###

* Support for Pausable/Resumable Asynchronous assemblies
* Add assembly files as Inputstream

### 0.0.6 / 2018-01-19 ###

* Do tus uploads only when there are files to upload.

### 0.0.5 / 2018-01-18 ###

* Check for assembly error before proceeding with tus upload

### 0.0.4 / 2018-01-08 ###

* Remove tus upload chunksize

### 0.0.3 / 2017-05-15 ###

* `Steps.removeStep` method
* Added example project for sample codes
* Maven compliant deployment build. 

### 0.0.2 / 2017-05-12 ###

* `AssemblyResponse.getStepResult` method

### 0.0.1 / 2017-05-09 ###

* Initial release
