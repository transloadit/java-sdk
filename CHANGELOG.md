### Not released yet ###
#### Major Release 
* Exchange the Socket based assembly status fetching with a Server-Sent-Events (SSE) solution.
* Added new methods to the AssemblyListener interface to provide more information about the assembly status. e.g. encoding progress with AssemblyListener#onAssemblyProgress().
* Changed existing methods in the AssemblyListener interface to provide the bare JSON response from the api instead of pre-parsed data.
* Removed the deprecated AsyncAssemblies class and functionality.

##### Breaking Changes - Upgrade Guide
* The AssemblyListener Interface has been upgraded. As a result you will have to (re-) implement the following methods:
  - `onFileUploadFinished(JSONObject uploadInformation);`
  - `onAssemblyProgress(JSONObject progress)`
  - `onAssemblyResultFinished(JSONArray result)`

* The AsyncAssemblies class has been removed. If you were using it, you will have to switch to the regular Assembly class.
  It has been extended with asynchronous upload capabilities in the past. 
The Example under `examples/src/main/java/com/transloadit/examples/MultiStepProcessing.java` shows how to use the new features.
### 1.0.1 / 2024-11-28 ###
* Added SDK support for generating signed Smart CDN URLs (see https://transloadit.com/docs/topics/signature-authentication/#smart-cdn).
  This functionality ships as Transloadit#getSignedSmartCDNUrl() - Method.
* Migrated test suite from JUnit4 to JUnit 5
* Upgrade okhttp to 4.12.0 as a security update

### 1.0.0 / 2022-12-14 ###
#### Major Release 
Warning: This version includes breaking changes and some experimental features, please keep that in mind when using it.

If you encounter any problems because of the upgrade, please do not hesitate to contact support@transloadit.com 
or open a GitHub-Issue.

##### Breaking Changes - Upgrade Guide
* The AssemblyListener Interface has been upgraded. As a result you will have to implement the following methods:
  - `onFileUploadPaused(String name)`
  - `onFileUploadResumed(String name)`
  - `onFileUploadProgress(long uploadedBytes, long totalBytes)`
  If you do not need their functionality, just leave them blank.
* Also take note of the deprecation of `AsyncAssemblies`. The normal `Assembly` class, thanks to its extended 
  functionality, serves as a replacement. You can find more about it further down in the text.

##### Most Important Innovations:
* Introduction of multithreaded uploads. - Now you can upload multiple files in parallel:
  * The uploads are pausable via `Assembly#pauseUploads()`.
  * And resumable with `Assembly#resumeUploads()`.
  * The default value of files being uploaded at the same time is 2. You can adjust this with 
  `Assembly#setMaxParallelUploads(int maxUploads)`.
  * If you want to turn off this feature use: `Assembly#setMaxParallelUploads(int maxUploads)` with a value of 1.
* The `AssemblyListener` has now an extended feature set and provides also information to the new upload mode.
* `AsyncAssemblies` are deprecated now in favor of multithreaded uploads.
 * Because some users, especially on Android, are using AsyncAssemblies 
   this release ships a fix for the corresponding Listeners to avoid `NullPointerExceptions`.
* If you want to add a `Step` to an `Assembly`, providing the Robot's name is now optional. This helps if you want to do a Template Override.
  The provided Examples were revised and new examples have been added.

##### Minor changes:
* All dependencies are up-to-date now and include all necessary security patches.
* Signature Authentication uses HmacSHA384 now.
* Signature Authentication uses a unique nonce per assembly in order to prevent signature reuse errors.

### 0.4.4 / 2022-10-30 ###
* The Socket-IO plugin has been updated to version 4, which is also used by the API.

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
