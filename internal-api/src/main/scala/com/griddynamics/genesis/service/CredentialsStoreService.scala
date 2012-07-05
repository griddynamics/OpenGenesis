package com.griddynamics.genesis.service

import com.griddynamics.genesis.api
import api.{ExtendedResult, RequestResult}

trait CredentialsStoreService {
  def get(projectId: Int, id: Int): Option[api.Credentials]
  def delete(projectId: Int, id: Int): RequestResult
  def list(projectId: Int): Iterable[api.Credentials]
  def create(creds: api.Credentials): ExtendedResult[api.Credentials]

  def find(projectId: Int, cloudProvider: String, keypairName: String): Option[api.Credentials]

  def generate(projectId: Int, cloudProvider: String, identity: String, credentials: String): api.Credentials

  def findCredentials(projectId: Int, cloudProvider: String, privateKey: String): Option[api.Credentials]

  def findCredentials(projectId: Int, cloudProvider: String): Seq[api.Credentials]

   def decrypt(creds: api.Credentials): api.Credentials
}