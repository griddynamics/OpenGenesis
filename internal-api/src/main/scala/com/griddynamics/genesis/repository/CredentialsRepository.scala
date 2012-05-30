package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api

trait CredentialsRepository {
  def findCredentials(projectId: Int, cloudProvider: String, privateKey: String): Option[api.Credentials]

  def save(entity: api.Credentials): api.Credentials

  def list(projectId: Int): Iterable[api.Credentials]

  def delete(projectId: Int, id: Int): Int

  def find(projectId: Int, cloudProvider: String, keyPairName: String): Option[api.Credentials]

  def find(cloudProvider: String, fingerPrint: String): Option[api.Credentials]

  def get(projectId: Int, id: Int): Option[api.Credentials]
}