import assert from 'node:assert/strict'

const IMPORTANT_STATION_NAMES = ['神池南站', '港口站']
const MINOR_STATION_VISUAL_SCALE = 0.6
const importantSet = new Set(IMPORTANT_STATION_NAMES)

function isImportantStation(name) {
  return importantSet.has(name)
}
function getStationVisualScale(name) {
  return isImportantStation(name) ? 1 : MINOR_STATION_VISUAL_SCALE
}
function scalePx(base, name) {
  return base * getStationVisualScale(name)
}

assert.equal(getStationVisualScale('神池南站'), 1)
assert.equal(getStationVisualScale('小站'), 0.6)
assert.equal(scalePx(10, '小站'), 6)
console.log('stationVisualScale.test.mjs: ok')
