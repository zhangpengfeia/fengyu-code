import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let args = CommandLine.arguments
let root = URL(fileURLWithPath: args[1])
let output = URL(fileURLWithPath: args[2])
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
var manifest: [[String: Any]] = []
for folder in ["12个星球", "第三册目录"] {
  let files = try FileManager.default.contentsOfDirectory(
    at: root.appendingPathComponent(folder), includingPropertiesForKeys: nil
  ).filter { $0.pathExtension == "pdf" }.sorted { $0.lastPathComponent < $1.lastPathComponent }
  for (index, url) in files.enumerated() {
    guard let doc = CGPDFDocument(url as CFURL), let page = doc.page(at: 1) else { continue }
    let rect = page.getBoxRect(.mediaBox)
    let scale = min(2.0, 2400.0 / max(rect.width, rect.height))
    let width = Int(ceil(rect.width * scale))
    let height = Int(ceil(rect.height * scale))
    guard
      let context = CGContext(
        data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
    else { continue }
    context.scaleBy(x: scale, y: scale)
    context.translateBy(x: -rect.minX, y: -rect.minY)
    context.drawPDFPage(page)
    let name = "\(folder == "12个星球" ? "planet" : "scene")-\(index).png"
    let target = output.appendingPathComponent(name)
    if let image = context.makeImage(),
      let dest = CGImageDestinationCreateWithURL(
        target as CFURL, UTType.png.identifier as CFString, 1, nil)
    {
      CGImageDestinationAddImage(dest, image, nil)
      CGImageDestinationFinalize(dest)
    }
    manifest.append([
      "file": name, "source": folder + "/" + url.lastPathComponent, "width": width,
      "height": height,
    ])
  }
}
let json = try JSONSerialization.data(
  withJSONObject: manifest, options: [.prettyPrinted, .sortedKeys])
try json.write(to: output.appendingPathComponent("manifest.json"))
print("Converted \(manifest.count) transparent PNG assets")
