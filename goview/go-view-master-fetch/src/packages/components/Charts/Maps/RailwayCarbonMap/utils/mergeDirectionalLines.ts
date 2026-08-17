import type { LineFeature } from '../types'

type ActiveDirection = 'all' | 'up' | 'down'

function segmentKey(line: LineFeature): string {
  const id = line.properties.sectionId ?? ''
  return id.replace(/-(UP|DOWN)$/i, '')
}

function average(a: number, b: number): number {
  return Math.round(((a + b) / 2) * 10) / 10
}

function mergePair(up: LineFeature, down: LineFeature): LineFeature {
  const upProps = up.properties
  const downProps = down.properties
  return {
    type: 'Feature',
    geometry: {
      type: 'LineString',
      coordinates: up.geometry.coordinates
    },
    properties: {
      ...upProps,
      direction: 'both',
      sectionId: segmentKey(up),
      carbonIntensity: average(upProps.carbonIntensity, downProps.carbonIntensity),
      energyIntensity: average(upProps.energyIntensity, downProps.energyIntensity),
      carbonEmission: average(upProps.carbonEmission, downProps.carbonEmission)
    }
  }
}

export function resolveLinesForDirection(
  allLines: LineFeature[],
  activeDirection: ActiveDirection
): LineFeature[] {
  if (activeDirection === 'up') {
    return allLines.filter(l => l.properties.direction === 'up')
  }
  if (activeDirection === 'down') {
    return allLines.filter(l => l.properties.direction === 'down')
  }

  const upByKey = new Map<string, LineFeature>()
  const downByKey = new Map<string, LineFeature>()

  for (const line of allLines) {
    const key = segmentKey(line)
    if (line.properties.direction === 'up') upByKey.set(key, line)
    else if (line.properties.direction === 'down') downByKey.set(key, line)
    else if (line.properties.direction === 'both') {
      return allLines.filter(l => l.properties.direction === 'both')
    }
  }

  const merged: LineFeature[] = []
  for (const [key, upLine] of upByKey) {
    const downLine = downByKey.get(key)
    merged.push(downLine ? mergePair(upLine, downLine) : upLine)
  }

  return merged.length > 0 ? merged : allLines.filter(l => l.properties.direction === 'both')
}
