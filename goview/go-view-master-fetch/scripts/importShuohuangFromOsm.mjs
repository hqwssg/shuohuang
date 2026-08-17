#!/usr/bin/env node
/**
 * 从 OpenStreetMap 导入朔黄线几何，生成 GeoJSON 与 data.json
 * Run: npm run import:shuohuang-railway
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ASSET_DIR = path.join(
  __dirname,
  '../src/packages/components/Charts/Maps/RailwayCarbonMap/assets'
)
const DATA_JSON = path.join(
  __dirname,
  '../src/packages/components/Charts/Maps/RailwayCarbonMap/data.json'
)

const FULL_STATION_ORDER = [
  '神池南站', '宁武西站', '龙宫站', '北大牛站', '原平南站', '回凤站', '东冶站', '南湾站',
  '滴流蹬站', '猴刎站', '小觉站', '古月站', '温塘站', '西柏坡站', '三汲站', '灵寿站',
  '行唐站', '新曲站', '定州西站', '定州东站', '安国站', '博野站', '蠡县站', '肃宁北站',
  '太师庄站', '河间站', '行别营站', '黎民居站', '杜生站', '沧州西站', '李天木站',
  '黄骅南站', '段庄站', '黄骅港站', '港口站'
]

const SHENCHI = [111.76, 39.09]
const HUANGHUA = [117.33, 38.37]
const SIMPLIFY_TOLERANCE = 0.0007

function haversineKm(a, b) {
  const R = 6371
  const dLat = ((b[1] - a[1]) * Math.PI) / 180
  const dLng = ((b[0] - a[0]) * Math.PI) / 180
  const lat1 = (a[1] * Math.PI) / 180
  const lat2 = (b[1] * Math.PI) / 180
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

function polylineLengthKm(coords) {
  let total = 0
  for (let i = 1; i < coords.length; i++) total += haversineKm(coords[i - 1], coords[i])
  return total
}

function avgLat(coords) {
  return coords.reduce((s, c) => s + c[1], 0) / coords.length
}

function dist2(a, b) {
  return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2
}

function findPositionOnPolyline(coords, point) {
  let best = { index: 0, t: 0, lng: coords[0][0], lat: coords[0][1], distanceKm: 0, dist2: Infinity }
  let walked = 0
  for (let i = 1; i < coords.length; i++) {
    const a = coords[i - 1]
    const b = coords[i]
    const segLen = haversineKm(a, b)
    const dx = b[0] - a[0]
    const dy = b[1] - a[1]
    const len2 = dx * dx + dy * dy
    let t = len2 > 0 ? ((point.lng - a[0]) * dx + (point.lat - a[1]) * dy) / len2 : 0
    t = Math.max(0, Math.min(1, t))
    const lng = a[0] + dx * t
    const lat = a[1] + dy * t
    const d2 = (lng - point.lng) ** 2 + (lat - point.lat) ** 2
    if (d2 < best.dist2) {
      best = { index: i - 1, t, lng, lat, distanceKm: walked + segLen * t, dist2: d2 }
    }
    walked += segLen
  }
  const { dist2: _d, ...rest } = best
  return rest
}

function perpDistance(point, lineStart, lineEnd) {
  const dx = lineEnd[0] - lineStart[0]
  const dy = lineEnd[1] - lineStart[1]
  if (dx === 0 && dy === 0) return Math.sqrt(dist2(point, lineStart))
  const t =
    ((point[0] - lineStart[0]) * dx + (point[1] - lineStart[1]) * dy) / (dx * dx + dy * dy)
  const proj = [lineStart[0] + t * dx, lineStart[1] + t * dy]
  return Math.sqrt(dist2(point, proj))
}

function douglasPeucker(coords, epsilon) {
  if (coords.length <= 2) return coords
  let maxDist = 0
  let maxIdx = 0
  for (let i = 1; i < coords.length - 1; i++) {
    const d = perpDistance(coords[i], coords[0], coords[coords.length - 1])
    if (d > maxDist) {
      maxDist = d
      maxIdx = i
    }
  }
  if (maxDist > epsilon) {
    const left = douglasPeucker(coords.slice(0, maxIdx + 1), epsilon)
    const right = douglasPeucker(coords.slice(maxIdx), epsilon)
    return [...left.slice(0, -1), ...right]
  }
  return [coords[0], coords[coords.length - 1]]
}

function orientChain(coords) {
  const dStart = dist2(coords[0], SHENCHI)
  const dEnd = dist2(coords[coords.length - 1], SHENCHI)
  if (dStart > dEnd) return [...coords].reverse()
  return coords
}

function buildChains(ways) {
  const nodeToWays = new Map()
  for (const w of ways) {
    for (const nid of w.nodes) {
      if (!nodeToWays.has(nid)) nodeToWays.set(nid, [])
      nodeToWays.get(nid).push(w)
    }
  }

  const used = new Set()
  const chains = []

  const wayCoords = w => w.geometry.map(g => [g.lon, g.lat])

  for (const w of ways) {
    if (used.has(w.id)) continue

    let coords = wayCoords(w)
    let frontNode = w.nodes[0]
    let backNode = w.nodes[w.nodes.length - 1]
    used.add(w.id)

    while (true) {
      const candidates = (nodeToWays.get(frontNode) || []).filter(x => !used.has(x.id))
      let next = candidates.find(c => c.nodes[c.nodes.length - 1] === frontNode)
      let reversed = false
      if (!next) {
        next = candidates.find(c => c.nodes[0] === frontNode)
        reversed = true
      }
      if (!next) break
      used.add(next.id)
      const nc = wayCoords(next)
      if (reversed) nc.reverse()
      coords = [...nc.slice(0, -1), ...coords]
      frontNode = next.nodes[0]
    }

    while (true) {
      const candidates = (nodeToWays.get(backNode) || []).filter(x => !used.has(x.id))
      let next = candidates.find(c => c.nodes[0] === backNode)
      let reversed = false
      if (!next) {
        next = candidates.find(c => c.nodes[c.nodes.length - 1] === backNode)
        reversed = true
      }
      if (!next) break
      used.add(next.id)
      const nc = wayCoords(next)
      if (reversed) nc.reverse()
      coords = [...coords, ...nc.slice(1)]
      backNode = next.nodes[next.nodes.length - 1]
    }

    if (coords.length >= 2) chains.push(coords)
  }

  return chains
}

function clusterWaysByLatitude(ways) {
  const withMid = ways.map(w => {
    const c = w.geometry.map(g => [g.lon, g.lat])
    return { way: w, midLat: avgLat(c) }
  })
  withMid.sort((a, b) => a.midLat - b.midLat)
  const mid = Math.floor(withMid.length / 2)
  const lower = withMid.slice(0, mid).map(x => x.way)
  const upper = withMid.slice(mid).map(x => x.way)
  return { lower, upper }
}

function splitUpDown(chains) {
  const longChains = chains
    .map(c => ({ coords: orientChain(c), len: polylineLengthKm(c), pts: c.length }))
    .filter(c => c.len > 200 && c.pts > 80)
    .sort((a, b) => b.len - a.len)

  if (longChains.length >= 2) {
    const sorted = [...longChains.slice(0, 4)].sort((a, b) => avgLat(b.coords) - avgLat(a.coords))
    return {
      up: douglasPeucker(sorted[0].coords, SIMPLIFY_TOLERANCE),
      down: douglasPeucker(sorted[1].coords, SIMPLIFY_TOLERANCE)
    }
  }

  if (longChains.length === 1) {
    const up = douglasPeucker(longChains[0].coords, SIMPLIFY_TOLERANCE)
    const down = up.map(([lng, lat]) => [lng, lat - 0.015])
    return { up, down }
  }

  return null
}

async function fetchOsmWays() {
  const query = `[out:json][timeout:120];
way[railway=rail]["name:en"="Shuohuang Line"](37.5,110,40,118);
out geom;`

  const endpoints = [
    'https://overpass.kumi.systems/api/interpreter',
    'https://overpass-api.de/api/interpreter'
  ]

  for (const url of endpoints) {
    try {
      const res = await fetch(url, {
        method: 'POST',
        body: new URLSearchParams({ data: query })
      })
      if (!res.ok) {
        console.warn(`${url} HTTP ${res.status}`)
        continue
      }
      const json = await res.json()
      const ways = json.elements.filter(e => e.type === 'way' && e.geometry?.length >= 2)
      if (ways.length > 0) return ways
    } catch (err) {
      console.warn(`${url} failed:`, err.message)
    }
  }

  return fetchOsmWaysFromCachedIds()
}

async function fetchOsmWaysFromCachedIds() {
  const cachePath = path.join(__dirname, '../../osm-query1.json')
  if (!fs.existsSync(cachePath)) {
    throw new Error('Overpass unavailable and no local osm-query1.json cache')
  }
  const cached = JSON.parse(fs.readFileSync(cachePath, 'utf8'))
  const ids = cached.elements
    .filter(e => e.type === 'way' && e.tags?.name === '朔黄线')
    .map(e => e.id)

  console.log(`Fallback: fetching geometry for ${ids.length} way ids...`)
  const batchSize = 80
  const ways = []

  for (let i = 0; i < ids.length; i += batchSize) {
    const batch = ids.slice(i, i + batchSize)
    const query = `[out:json][timeout:120];way(id:${batch.join(',')});out geom;`
    const res = await fetch('https://overpass.kumi.systems/api/interpreter', {
      method: 'POST',
      body: new URLSearchParams({ data: query })
    })
    if (!res.ok) {
      console.warn(`Batch ${i / batchSize + 1} HTTP ${res.status}, waiting 8s...`)
      await sleep(8000)
      continue
    }
    const json = await res.json()
    ways.push(...json.elements.filter(e => e.type === 'way' && e.geometry?.length >= 2))
    await sleep(2500)
  }

  if (ways.length > 100) return ways
  throw new Error(`Batch fetch incomplete (${ways.length} ways)`)
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function loadExistingGeojsonRoutes() {
  const p = path.join(ASSET_DIR, 'shuohuang-railway-osm.geojson')
  if (!fs.existsSync(p)) return null
  try {
    const geo = JSON.parse(fs.readFileSync(p, 'utf8'))
    const up = geo.features.find(f => f.properties?.direction === 'up')
    const down = geo.features.find(f => f.properties?.direction === 'down')
    if (!up?.geometry?.coordinates?.length || !down?.geometry?.coordinates?.length) return null
    return {
      up: up.geometry.coordinates,
      down: down.geometry.coordinates,
      source: 'cached-geojson'
    }
  } catch {
    return null
  }
}

function loadLocalRailGeomFile() {
  const geomPath = path.join(__dirname, '../../osm-rail-geom.json')
  if (!fs.existsSync(geomPath)) return null
  try {
    const json = JSON.parse(fs.readFileSync(geomPath, 'utf8'))
    const ways = json.elements.filter(
      e => e.type === 'way' && e.tags?.name === '朔黄线' && e.geometry?.length >= 2
    )
    return ways.length > 100 ? ways : null
  } catch {
    return null
  }
}

function densifyCoords(coords, stepsPerSeg = 24) {
  if (coords.length < 2) return coords
  const out = []
  for (let i = 0; i < coords.length - 1; i++) {
    const a = coords[i]
    const b = coords[i + 1]
    for (let s = 0; s < stepsPerSeg; s++) {
      const t = s / stepsPerSeg
      out.push([a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t])
    }
  }
  out.push(coords[coords.length - 1])
  return out
}

/** 公开走向控制点（OSM 不可用时的离线骨架，沿朔黄正线大势） */
function buildSkeletonRoutes() {
  const upCtrl = [
    [111.76, 39.09],
    [112.0, 39.05],
    [112.5, 38.95],
    [113.0, 38.85],
    [113.5, 38.75],
    [113.65, 38.72],
    [113.85, 38.68],
    [114.0, 38.65],
    [114.15, 38.6],
    [114.25, 38.55],
    [114.5, 38.55],
    [114.7, 38.5],
    [114.85, 38.47],
    [115.0, 38.48],
    [115.25, 38.44],
    [115.5, 38.42],
    [115.8, 38.35],
    [116.0, 38.25],
    [116.3, 38.18],
    [116.5, 38.15],
    [116.75, 38.22],
    [116.95, 38.28],
    [117.15, 38.34],
    [117.33, 38.37],
    [117.4, 38.35]
  ]
  const up = densifyCoords(upCtrl, 28)
  const down = up.map(([lng, lat]) => [lng, lat - 0.018])
  return { up, down, source: 'skeleton' }
}

const STATION_CACHE = path.join(ASSET_DIR, 'osm-stations-cache.json')

const STATION_NAME_ALIASES = {
  '神池南站': ['神池南', '神池南站', 'Shenchi South', 'Shenchinan'],
  '宁武西站': ['宁武西', '宁武西站', 'Ningwu West', 'Ningwuxi'],
  '龙宫站': ['龙宫', '龙宫站', 'Longgong'],
  '北大牛站': ['北大牛', '北大牛站'],
  '原平南站': ['原平南', '原平南站', 'Yuanping South'],
  '回凤站': ['回凤', '回凤站'],
  '东冶站': ['东冶', '东冶站'],
  '南湾站': ['南湾', '南湾站'],
  '滴流蹬站': ['滴流蹬', '滴流蹬站'],
  '猴刎站': ['猴刎', '猴刎站'],
  '小觉站': ['小觉', '小觉站'],
  '古月站': ['古月', '古月站'],
  '温塘站': ['温塘', '温塘站'],
  '西柏坡站': ['西柏坡', '西柏坡站', 'Xibaipo'],
  '三汲站': ['三汲', '三汲站'],
  '灵寿站': ['灵寿', '灵寿站'],
  '行唐站': ['行唐', '行唐站'],
  '新曲站': ['新曲', '新曲站'],
  '定州西站': ['定州西', '定州西站', 'Dingzhou West'],
  '定州东站': ['定州东', '定州东站', 'Dingzhou East'],
  '安国站': ['安国', '安国站'],
  '博野站': ['博野', '博野站'],
  '蠡县站': ['蠡县', '蠡县站', 'Lixian'],
  '肃宁北站': ['肃宁北', '肃宁北站', 'Suning North'],
  '太师庄站': ['太师庄', '太师庄站'],
  '河间站': ['河间', '河间站'],
  '行别营站': ['行别营', '行别营站'],
  '黎民居站': ['黎民居', '黎民居站'],
  '杜生站': ['杜生', '杜生站'],
  '沧州西站': ['沧州西', '沧州西站', 'Cangzhou West'],
  '李天木站': ['李天木', '李天木站'],
  '黄骅南站': ['黄骅南', '黄骅南站'],
  '段庄站': ['段庄', '段庄站'],
  '黄骅港站': ['黄骅港', '黄骅港站', 'Huanghua Port'],
  '港口站': ['港口', '港口站', 'Gangkou']
}

function normalizeName(s) {
  return (s || '').replace(/站$/, '').replace(/\s+/g, '').toLowerCase()
}

function matchOsmNode(standardName, osmNodes) {
  const aliases = STATION_NAME_ALIASES[standardName] || [standardName]
  const keys = new Set(aliases.map(normalizeName))
  keys.add(normalizeName(standardName))
  for (const node of osmNodes) {
    const names = [node.tags?.name, node.tags?.['name:zh'], node.tags?.['name:en']].filter(Boolean)
    for (const n of names) {
      const nn = normalizeName(n)
      if (keys.has(nn) || [...keys].some(k => nn.includes(k) || k.includes(nn))) {
        return node
      }
    }
  }
  return null
}

async function fetchOsmStationNodes() {
  if (fs.existsSync(STATION_CACHE)) {
    const age = Date.now() - fs.statSync(STATION_CACHE).mtimeMs
    if (age < 7 * 24 * 3600 * 1000) {
      return JSON.parse(fs.readFileSync(STATION_CACHE, 'utf8')).elements
    }
  }
  const nodes = []
  for (const query of [
    `[out:json][timeout:90];node[railway=station](37.5,110,40,118);out body;`,
    `[out:json][timeout:90];node[railway=halt](37.5,110,40,118);out body;`
  ]) {
    try {
      const res = await fetch('https://overpass.kumi.systems/api/interpreter', {
        method: 'POST',
        body: new URLSearchParams({ data: query })
      })
      if (res.ok) {
        const json = await res.json()
        nodes.push(...json.elements.filter(e => e.type === 'node' && e.lat && e.lon))
      }
    } catch (e) {
      console.warn('OSM station fetch failed:', e.message)
    }
  }
  if (nodes.length > 0) {
    fs.mkdirSync(ASSET_DIR, { recursive: true })
    fs.writeFileSync(STATION_CACHE, JSON.stringify({ elements: nodes }, null, 2))
  }
  return nodes
}

function loadManualStations() {
  const p = path.join(ASSET_DIR, 'shuohuang-stations-manual.json')
  if (!fs.existsSync(p)) return new Map()
  const data = JSON.parse(fs.readFileSync(p, 'utf8'))
  return new Map(data.stations.map(s => [s.name, s]))
}

function findPositionOnPolylineFromIndex(coords, point, minIndex = 0) {
  let best = {
    index: minIndex,
    t: 0,
    lng: coords[minIndex][0],
    lat: coords[minIndex][1],
    distanceKm: 0,
    dist2: Infinity
  }
  let walked = 0
  for (let i = 1; i < coords.length; i++) {
    const a = coords[i - 1]
    const b = coords[i]
    const segLen = haversineKm(a, b)
    if (i - 1 >= minIndex) {
      const dx = b[0] - a[0]
      const dy = b[1] - a[1]
      const len2 = dx * dx + dy * dy
      let t = len2 > 0 ? ((point.lng - a[0]) * dx + (point.lat - a[1]) * dy) / len2 : 0
      t = Math.max(0, Math.min(1, t))
      const lng = a[0] + dx * t
      const lat = a[1] + dy * t
      const d2 = (lng - point.lng) ** 2 + (lat - point.lat) ** 2
      if (d2 < best.dist2) {
        best = { index: i - 1, t, lng, lat, distanceKm: walked + segLen * t, dist2: d2 }
      }
    }
    walked += segLen
  }
  const { dist2: _d, ...rest } = best
  return rest
}

function resolveStationCoordinates(upCoords, osmNodes, manualMap) {
  const stations = []
  const stats = { osm: 0, manual: 0 }
  let minIndex = 0

  for (const name of FULL_STATION_ORDER) {
    let lng, lat, source
    const osm = matchOsmNode(name, osmNodes)
    if (osm) {
      lng = osm.lon
      lat = osm.lat
      source = 'osm'
      stats.osm++
    } else if (manualMap.has(name)) {
      const m = manualMap.get(name)
      lng = m.lng
      lat = m.lat
      source = 'manual'
      stats.manual++
    } else {
      console.error(`No coordinates for ${name}`)
      process.exit(1)
    }

    const pos = findPositionOnPolylineFromIndex(upCoords, { lng, lat }, minIndex)
    minIndex = Math.max(minIndex, pos.index)
    const distanceToLineM = Math.round(haversineKm([lng, lat], [pos.lng, pos.lat]) * 1000)

    stations.push({
      name,
      lng: Math.round(pos.lng * 10000) / 10000,
      lat: Math.round(pos.lat * 10000) / 10000,
      chainageKm: Math.round(pos.distanceKm * 10) / 10,
      source,
      distanceToLineM
    })
  }

  for (let i = 1; i < stations.length; i++) {
    if (stations[i].chainageKm <= stations[i - 1].chainageKm) {
      console.error(
        `Chainage not monotonic: ${stations[i - 1].name}(${stations[i - 1].chainageKm}) >= ${stations[i].name}(${stations[i].chainageKm})`
      )
      process.exit(1)
    }
  }

  return { stations, stats }
}

function extendPolylineWestIfNeeded(upCoords, downCoords, firstStation) {
  const lineStart = upCoords[0]
  const gapKm = haversineKm(lineStart, [firstStation.lng, firstStation.lat])
  if (firstStation.chainageKm < 1 && gapKm > 5) {
    const latDelta = upCoords[0][1] - downCoords[0][1]
    const downStart = [firstStation.lng, firstStation.lat - latDelta]
    return {
      up: [[firstStation.lng, firstStation.lat], ...upCoords],
      down: [downStart, ...downCoords],
      extendedKm: gapKm
    }
  }
  return { up: upCoords, down: downCoords, extendedKm: 0 }
}

function reportAdjacentDistances(stations) {
  const dists = []
  for (let i = 1; i < stations.length; i++) {
    dists.push(
      haversineKm(
        [stations[i - 1].lng, stations[i - 1].lat],
        [stations[i].lng, stations[i].lat]
      )
    )
  }
  const avg = dists.reduce((a, b) => a + b, 0) / dists.length
  const variance =
    dists.reduce((s, x) => s + (x - avg) ** 2, 0) / dists.length
  console.log(
    `adjacent km: min=${Math.min(...dists).toFixed(1)} max=${Math.max(...dists).toFixed(1)} avg=${avg.toFixed(1)} std=${Math.sqrt(variance).toFixed(1)}`
  )
}

function emitDataJson(upCoords, downCoords, stations) {
  const upLen = Math.round(polylineLengthKm(upCoords) * 10) / 10
  const downLen = Math.round(polylineLengthKm(downCoords) * 10) / 10

  const points = stations.map((s, i) => ({
    lng: s.lng,
    lat: s.lat,
    name: s.name,
    type: s.name === '港口站' ? 'marshalling' : 'station',
    latestAccountingTime: '2026-05-01',
    carbonIntensity: Math.round((8.2 + 0.8 * Math.sin(i * 0.45) + (i % 5) * 0.06) * 10) / 10,
    carbonEmission: Math.round(500 + (i % 7) * 280 + Math.sin(i) * 200),
    unit: 'tCO2e'
  }))

  const lineFeature = (direction, coords, len, id) => ({
    type: 'Feature',
    properties: {
      sectionName: '朔黄铁路全线',
      direction,
      sectionId: id,
      energyIntensity: direction === 'up' ? 28.5 : 26.2,
      carbonIntensity: direction === 'up' ? 8.8 : 8.2,
      statPeriod: '2026年5月',
      lengthKm: len
    },
    geometry: {
      type: 'LineString',
      coordinates: coords.map(([lng, lat]) => [
        Math.round(lng * 100000) / 100000,
        Math.round(lat * 100000) / 100000
      ])
    }
  })

  return {
    points: { points, statPeriod: '2026年5月' },
    lines: {
      type: 'FeatureCollection',
      features: [
        lineFeature('up', upCoords, upLen, 'SHH-FULL-UP'),
        lineFeature('down', downCoords, downLen, 'SHH-FULL-DOWN')
      ]
    },
    meta: {
      lineName: '朔黄铁路',
      totalLengthKm: 594,
      sectionCount: 34,
      pointCount: 35,
      statPeriod: '2026年5月',
      updateTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      stationOrder: FULL_STATION_ORDER
    }
  }
}

async function main() {
  console.log('Fetching OSM Shuohuang Line...')
  let up
  let down
  let dataSource = 'osm'

  const localWays = loadLocalRailGeomFile()
  let ways = localWays
  if (!ways) {
    try {
      ways = await fetchOsmWays()
    } catch (err) {
      console.warn('Primary fetch failed:', err.message)
    }
  }
  if (!ways) {
    const cachedRoutes = loadExistingGeojsonRoutes()
    if (cachedRoutes) {
      console.log('Skipping way fetch; will refresh stations from cached geojson')
    } else {
      try {
        ways = await fetchOsmWaysFromCachedIds()
      } catch (err2) {
        console.warn('Batch fetch failed:', err2.message)
      }
    }
  }

  if (ways && ways.length > 0) {
    console.log(`ways: ${ways.length}`)
    const { lower, upper } = clusterWaysByLatitude(ways)
    const upChains = buildChains(upper)
    const downChains = buildChains(lower)
    let split = splitUpDown([...upChains, ...downChains])
    if (!split) {
      split = splitUpDown(buildChains(ways))
    }
    if (split) {
      up = orientChain(split.up)
      down = orientChain(split.down)
    }
  }

  if (!up || !down) {
    const cached = loadExistingGeojsonRoutes()
    if (cached) {
      up = cached.up
      down = cached.down
      dataSource = cached.source
      console.log('Using cached shuohuang-railway-osm.geojson')
    }
  }

  if (!up || !down) {
    console.warn('Using skeleton route fallback (run import again when OSM is available)')
    const sk = buildSkeletonRoutes()
    up = sk.up
    down = sk.down
    dataSource = sk.source
  }

  up = orientChain(up)
  down = orientChain(down)

  if (dist2(up[0], SHENCHI) > dist2(up[up.length - 1], SHENCHI)) up.reverse()
  if (dist2(down[0], SHENCHI) > dist2(down[down.length - 1], SHENCHI)) down.reverse()

  const upKmInitial = polylineLengthKm(up)
  const downKmInitial = polylineLengthKm(down)

  console.log(`source: ${dataSource}`)
  console.log(`up: ${upKmInitial.toFixed(1)} km, ${up.length} points`)
  console.log(`down: ${downKmInitial.toFixed(1)} km, ${down.length} points`)

  fs.mkdirSync(ASSET_DIR, { recursive: true })

  const osmNodes = await fetchOsmStationNodes()
  const manualMap = loadManualStations()
  let { stations, stats } = resolveStationCoordinates(up, osmNodes, manualMap)

  const ext = extendPolylineWestIfNeeded(up, down, stations[0])
  up = ext.up
  down = ext.down
  if (ext.extendedKm > 0) {
    console.log(`Extended west by ${ext.extendedKm.toFixed(1)} km`)
    ;({ stations, stats } = resolveStationCoordinates(up, osmNodes, manualMap))
  }

  console.log(`sources: osm ${stats.osm}, manual ${stats.manual}`)
  console.log(`stations: ${stations.length}/${FULL_STATION_ORDER.length}`)
  reportAdjacentDistances(stations)

  let upKm = polylineLengthKm(up)
  let downKm = polylineLengthKm(down)

  const geojson = {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {
          sectionName: '朔黄铁路全线',
          direction: 'up',
          sectionId: 'SHH-FULL-UP',
          lengthKm: Math.round(upKm * 10) / 10
        },
        geometry: { type: 'LineString', coordinates: up }
      },
      {
        type: 'Feature',
        properties: {
          sectionName: '朔黄铁路全线',
          direction: 'down',
          sectionId: 'SHH-FULL-DOWN',
          lengthKm: Math.round(downKm * 10) / 10
        },
        geometry: { type: 'LineString', coordinates: down }
      }
    ]
  }

  fs.writeFileSync(
    path.join(ASSET_DIR, 'shuohuang-railway-osm.geojson'),
    JSON.stringify(geojson, null, 2)
  )
  fs.writeFileSync(
    path.join(ASSET_DIR, 'shuohuang-stations.generated.json'),
    JSON.stringify({ stations, generatedAt: new Date().toISOString() }, null, 2)
  )

  const dataJson = emitDataJson(up, down, stations)
  fs.writeFileSync(DATA_JSON, JSON.stringify(dataJson, null, 2))
  console.log(`Wrote data.json (${dataJson.points.points.length} stations)`)
  console.log('Done.')
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
