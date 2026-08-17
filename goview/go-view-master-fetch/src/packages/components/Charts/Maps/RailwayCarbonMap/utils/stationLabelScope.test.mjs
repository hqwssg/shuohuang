import assert from 'node:assert/strict'

const IMPORTANT_STATION_NAMES = [
  '神池南站',
  '宁武西站',
  '原平南站',
  '西柏坡站',
  '定州西站',
  '肃宁北站',
  '沧州西站',
  '黄骅港站',
  '港口站'
]

const importantSet = new Set(IMPORTANT_STATION_NAMES)

function shouldShowStationLabel(stationName, scope, showPointLabels) {
  if (!showPointLabels) return false
  if (scope === 'all') return true
  return importantSet.has(stationName)
}

assert.equal(shouldShowStationLabel('神池南站', 'important', true), true)
assert.equal(shouldShowStationLabel('龙宫站', 'important', true), false)
assert.equal(shouldShowStationLabel('龙宫站', 'all', true), true)
assert.equal(shouldShowStationLabel('神池南站', 'important', false), false)

console.log('stationLabelScope.test.mjs: ok')
