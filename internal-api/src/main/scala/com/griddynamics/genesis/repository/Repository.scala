package com.griddynamics.genesis.repository

import com.griddynamics.genesis.model.GenesisEntity

trait Repository[Api] {
  def load(id: Int): Api

  def get(id: Int): Option[Api]

  def list: List[Api]

  def delete(entity: Api): Int

  def delete(id: GenesisEntity.Id): Int

  def save(entity: Api): Api

  def insert(entity: Api): Api

  def update(entity: Api): Api
}