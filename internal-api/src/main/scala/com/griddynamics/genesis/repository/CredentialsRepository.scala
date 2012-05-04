package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api

trait CredentialsRepository {
  def findCredentials(projectId: Int, cloudProvider: String, privateKey: String): Option[api.Credentials]

  def save(entity: api.Credentials): api.Credentials

  def list(projectId: Int): Iterable[api.Credentials]

  def delete(key: Int): Int

  def find(projectId: Int, cloudProvider: String, name: String): Option[api.Credentials]

  def find(cloudProvider: String, fingerPrint: String): Option[api.Credentials]

  def get(id: Int): Option[api.Credentials]
}