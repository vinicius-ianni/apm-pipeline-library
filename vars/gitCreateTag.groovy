// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

/**
  Create a git TAG named ${BUILD_TAG} and push it to the git repo.
  It requires to initialise the pipeline with githubEnv() first.

  gitCreateTag()
*/

def call(Map params = [:]) {
  def tag =  params.get('tag', "${BUILD_TAG}")
  def tagArgs =  params.get('tagArgs', '')
  def pushArgs = params.get('pushArgs', '')
  def credentialsId = params.get('credentialsId', '')
  sh(label: "create tag", script: "git tag -a -m 'chore: Create tag ${tag}' '${tag}' ${tagArgs}")
  gitPush(credentialsId: credentialsId, args: "--tags ${pushArgs}")
}
