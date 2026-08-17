import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  getStationScope,
  setStationScope,
  createScopeParamsHolder,
  getScopeGeneration
} from './stationScopeController.ts'

test('default scope is overall', () => {
  const holder = createScopeParamsHolder()
  assert.deepEqual(getStationScope(holder), { dataScope: 'overall', stationName: '' })
})

test('setStationScope station writes name', () => {
  const holder = createScopeParamsHolder()
  assert.equal(setStationScope(holder, 'station', '神池南站'), true)
  assert.deepEqual(getStationScope(holder), { dataScope: 'station', stationName: '神池南站' })
})

test('duplicate set returns false', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '神池南站')
  assert.equal(setStationScope(holder, 'station', '神池南站'), false)
})

test('set overall clears stationName', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '宁武西站')
  setStationScope(holder, 'overall')
  assert.deepEqual(getStationScope(holder), { dataScope: 'overall', stationName: '' })
})

test('initial scopeGeneration is 0', () => {
  const holder = createScopeParamsHolder()
  assert.equal(getScopeGeneration(holder), 0)
})

test('successful set increments scopeGeneration', () => {
  const holder = createScopeParamsHolder()
  assert.equal(setStationScope(holder, 'station', '神池南站'), true)
  assert.equal(getScopeGeneration(holder), 1)
  assert.equal(setStationScope(holder, 'overall'), true)
  assert.equal(getScopeGeneration(holder), 2)
})

test('duplicate set does not increment scopeGeneration', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '神池南站')
  assert.equal(getScopeGeneration(holder), 1)
  assert.equal(setStationScope(holder, 'station', '神池南站'), false)
  assert.equal(getScopeGeneration(holder), 1)
})
