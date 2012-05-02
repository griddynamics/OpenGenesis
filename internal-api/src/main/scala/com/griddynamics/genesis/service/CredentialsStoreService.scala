package com.griddynamics.genesis.service

import com.griddynamics.genesis.api
import api.RequestResult

trait CredentialsStoreService {
  def get(id: Int): RequestResult

  def delete(id: Int): RequestResult

  def list(projectId: Int): Iterable[api.Credentials]

  def create(creds: api.Credentials): RequestResult

  def update(creds: api.Credentials): RequestResult
}