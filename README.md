# Camera2VisionFX

This sample will let you use the basic functionalities of both Camera1 API and Camera2 API, but with a Google Vision Face Detector added.
You will get all the power of Google Vision Services on your Camera1/Camera2 app.
Also, in this version I have added live preview and picture taking with Filters provided by AndroidGPUImage lib.
As always you can switch between Front and Back cameras, take pics and record video (without filters).

## Pre-requisites

* Android SDK 26+
* Android Build Tools 26+
* Android Support Repository

## Issues

Maybe you fill find out that the captured picture is flipped on some devices. You will have to read the EXIF data embedded in the picture
to acquire rotation info and flip manually with a Matrix.

## Tests

Tested and working on:
* Samsung Galaxy S4 (API Level: 22)

Please let me know if you test with success on your devices so I can update this list.

## License

Copyright 2017 Minniti Ezequiel Adrian

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
