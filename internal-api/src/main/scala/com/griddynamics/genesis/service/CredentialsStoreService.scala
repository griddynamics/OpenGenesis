package com.griddynamics.genesis.service

import com.griddynamics.genesis.api
import api.{ExtendedResult}

trait CredentialsStoreService {
  def get(projectId: Option[Int], id: Int): Option[api.Credentials]
  def delete(projectId: Option[Int], id: Int): ExtendedResult[Int]
  def list(projectId: Option[Int]): Iterable[api.Credentials]
  def create(creds: api.Credentials): ExtendedResult[api.Credentials]
  def find(projectId: Option[Int], cloudProvider: String, keypairName: String): Option[api.Credentials]
  def generate(projectId: Option[Int], cloudProvider: String, identity: String, credentials: String): api.Credentials
  def findCredentials(projectId: Option[Int], cloudProvider: String, privateKey: String): Option[api.Credentials]
  def findCredentials(projectId: Option[Int], cloudProvider: String): Seq[api.Credentials]
  def decrypt(creds: api.Credentials): api.Credentials
}