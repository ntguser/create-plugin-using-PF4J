//package org.pf4j.demo.welcome;
//
//import java.util.HashMap;
//import org.apache.commons.lang.StringUtils;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.pf4j.Extension;
//import org.pf4j.Plugin;
//import org.pf4j.PluginWrapper;
//import org.pf4j.RuntimeMode;
//import org.pf4j.demo.api.GenericPluginInterface;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
//import org.apache.poi.xssf.usermodel.XSSFCell;
//import org.apache.poi.xssf.usermodel.XSSFRow;
//import org.apache.poi.xssf.usermodel.XSSFSheet;
//import org.json.simple.JSONObject;
//
//public class TSSRTemplateMapping extends Plugin {
//
//	public TSSRTemplateMapping(PluginWrapper wrapper) {
//		super(wrapper);
//	}
//
//	@Override
//	public void start() {
//		System.out.println("TSSRPlugin.start()");
//		// for testing the development mode
//		if (RuntimeMode.DEPLOYMENT.equals(wrapper.getRuntimeMode())) {
//			System.out.println(StringUtils.upperCase("TSSRPlugin"));
//		}
//	}
//
//	@Override
//	public void stop() {
//		System.out.println("TSSRPlugin.stop()");
//	}
//
//	@Extension
//	public static class TSSRTemplateMappingExtension implements GenericPluginInterface {
//
//		@Override
//		public HashMap<String, Object> runPlugin(HashMap<String, Object> inp) {
//			HashMap<String, Object> out = new HashMap<String, Object>();
//
////            long x = Long.parseLong(inp.get("x").toString());
////            long y = Long.parseLong(inp.get("y").toString());
////            long result = x + y;
////            out.put("z", result);
//			try {
//				out = MapTSSRTemptoUDAS(inp);
//				out.put("plugin_output", "" + out.get("plugin_output"));
//			} catch (InvalidFormatException e) {
//				out.put("plugin_output", e);
//				e.printStackTrace();
//			} catch (IOException e) {
//				out.put("plugin_output", e);
//				e.printStackTrace();
//			}
//			return out;
//		}
//
//		public HashMap<String, Object> MapTSSRTemptoUDAS(HashMap<String, Object> plugin_inp)
//				throws InvalidFormatException, IOException {
//			List<JSONObject> dataList = new ArrayList<JSONObject>();
//			String[] files = (String[]) plugin_inp.get("inp");
//			XSSFWorkbook uDASWorkBook = new XSSFWorkbook(new File(files[0]));
//			XSSFSheet UDASWorkSheet = uDASWorkBook.getSheetAt(0);
//			XSSFRow header = UDASWorkSheet.getRow(0);
//			dataList = iterateInUDASFile(UDASWorkSheet, files[1]);
//			HashMap<String, Object> pluginOut = new HashMap<String, Object>();
//			pluginOut.put("plugin_output", dataList);
//			return pluginOut;
//		}
//
//		public List<JSONObject> iterateInUDASFile(XSSFSheet UDASWorkSheet, String templateUrl)
//				throws InvalidFormatException, IOException {
//			List<JSONObject> dataList = new ArrayList<JSONObject>();
//			XSSFRow header = UDASWorkSheet.getRow(0);
//
//			for (int rowIndex = 1; rowIndex < UDASWorkSheet.getPhysicalNumberOfRows(); rowIndex++) {
//				XSSFRow row = UDASWorkSheet.getRow(rowIndex);
//				JSONObject rowJsonObject = new JSONObject();
//				if (row != null && cellNotEmpty(row.getCell(0))) {
//					for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
////						System.out.println(
////								"row:" + rowIndex + "  col:" + colIndex + " UDA Name" + row.getCell(0).toString());
//						if (cellNotEmpty(row.getCell(colIndex))) {
//							rowJsonObject.put(
//									header.getCell(colIndex).toString().trim().toLowerCase().replace(" ", "_"),
//									row.getCell(colIndex).toString());
//						}
//					}
//					int udaTypeId = UDAType.TEXT;
//					String udaValue = "";
//					if (row.getCell(2).toString().contains("Numeric")) {
//						udaTypeId = UDAType.NUMERIC;
//						udaValue = getUDAValueFromTemplate(rowJsonObject, templateUrl);
//
//					} else if (row.getCell(2).toString().contains("Text Field")) {
//						udaTypeId = UDAType.TEXT;
//						udaValue = getUDAValueFromTemplate(rowJsonObject, templateUrl);
//
//					} else if (row.getCell(2).toString().contains("Value List")) {
//						udaTypeId = UDAType.VALUE_LIST;
//						udaValue = getSelectedValueFromValue_list(rowJsonObject, templateUrl);
//
//					} else if (row.getCell(2).toString().contains("Grid")) {
//						udaTypeId = UDAType.GRID;
//						rowIndex++;
//						JSONObject obj = getGridCols(UDASWorkSheet, rowIndex, header, templateUrl);
//						List<JSONObject> gridCols = getGridValues(rowJsonObject, templateUrl,
//								(List<JSONObject>) obj.get("gridCols"));
//						rowJsonObject.put("grid_cols", obj.get("gridCols"));
//						rowIndex = (Integer) obj.get("lastIndex") - 1;// because it will be incremented by loop
//
//					} else if (row.getCell(2).toString().contains("Check Box")) {
//						udaTypeId = UDAType.CHECK_BOX;
//						udaValue = getUDAValueFromTemplate(rowJsonObject, templateUrl);
//					}
//
//					if (!rowJsonObject.isEmpty()) {
//						rowJsonObject.put("uda_type_id", udaTypeId);
//						rowJsonObject.put("uda_value", udaValue);
//						dataList.add(rowJsonObject);
//					}
//				}
//			}
////			System.out.println(dataList);
//			return dataList;
//		}
//
//		private String getSelectedValueFromValue_list(JSONObject uda, String templateUrl)
//				throws InvalidFormatException, IOException {
//			XSSFWorkbook workBook = new XSSFWorkbook(new File(templateUrl));
//			int sheetIndex = 1;
//			while (sheetIndex <= 2) {
//				XSSFSheet workSheet = workBook.getSheetAt(sheetIndex);
//				int end = workSheet.getPhysicalNumberOfRows();
//				int colStart = 0;
//				boolean skip = false;
//				for (int rowIndex = 0; rowIndex < end; rowIndex++) {
//					XSSFRow row = workSheet.getRow(rowIndex);
//					if (row != null) {
//						String udaNameInSheet = uda.get("name_in_excel") != null ? uda.get("name_in_excel").toString()
//								: uda.get("name").toString();
//						if (row.getCell(colStart) != null
//								&& (row.getCell(colStart).toString().contains(udaNameInSheet) || skip)) {
////							System.out.println("found value list: " + uda.get("name"));
//							colStart = 1;
//							// calculate range of values
//							int start = rowIndex;
//							XSSFRow tempRow = workSheet.getRow(rowIndex + 1);
//							if (!skip)
//								end = rowIndex;
//							while (tempRow.getCell(0) == null || tempRow.getCell(0).toString().isEmpty() && !skip) {
//								end++;
//								tempRow = workSheet.getRow(end);
//							}
//							skip = true;
//							for (int colIndex = 1; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
////								System.out.println("row:" + rowIndex + " col:" + colIndex);
//								if (cellNotEmpty(row.getCell(colIndex))) {
//									String columnValue;
//									try {
//										row.getCell(colIndex).getCellFormula();
//										columnValue = row.getCell(colIndex) != null
//												? row.getCell(colIndex).getRawValue().toString()
//												: " ";
//
//									} catch (Exception e) {
//										columnValue = row.getCell(colIndex) != null ? row.getCell(colIndex).toString()
//												: " ";
//									}
//									if (columnValue.toUpperCase().equals("Y")) {
////										System.out.println(
////												"selected " + uda.get("name") + "udaValue: " + row.getCell(colIndex));
//										return row.getCell(colIndex - 1).toString();
//									}
//								}
//							}
//						}
//					}
//				}
//				sheetIndex++;
//			}
//			return null;
//		}
//
//		private List<JSONObject> getGridValues(JSONObject uda, String templateUrl, List<JSONObject> gridCols)
//				throws InvalidFormatException, IOException {
//			XSSFWorkbook workBook = new XSSFWorkbook(new File(templateUrl));
//			int sheetIndex = 1;
//			while (sheetIndex <= 2) {
//				XSSFSheet workSheet = workBook.getSheetAt(sheetIndex);
////				System.out.println("sheetIndex" + sheetIndex);
//				for (int i = 0; i < workSheet.getPhysicalNumberOfRows(); i++) {
//					XSSFRow row = workSheet.getRow(i);
//					if (row != null && cellNotEmpty(row.getCell(0))) {
//						for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
////							System.out.println("row:" + i + " col:" + colIndex);
//							if (cellNotEmpty(row.getCell(colIndex))) {
//								String columnValue;
//								try {
//									row.getCell(colIndex).getCellFormula();
//									columnValue = row.getCell(colIndex) != null
//											? row.getCell(colIndex).getRawValue().toString()
//											: " ";
//
//								} catch (Exception e) {
//									columnValue = row.getCell(colIndex) != null ? row.getCell(colIndex).toString()
//											: " ";
//								}
//								if (columnValue.contains((String) uda.get("name").toString().replace("&", "and"))) {// grid
//																													// is
//																													// found
////									System.out.println("found Grid UDA: " + uda.get("name"));
//
//									if (uda.get("name").toString().equals("RRU Type & Quantity")) {// static handling
//																									// for
//																									// RRU
//																									// Type & Quantity
//																									// grid
////										System.out.println("found Grid UDA: " + uda.get("name"));
//										String type = "";
//										String quantity = "";
//										colIndex++;
//										while (colIndex + 1 < row.getPhysicalNumberOfCells()) {
//											try {
//												row.getCell(colIndex).getCellFormula();
//												type += row.getCell(colIndex) != null
//														? row.getCell(colIndex).getRawValue().toString() + ";"
//														: " ";
//
//											} catch (Exception e) {
//												type += row.getCell(colIndex) != null
//														? row.getCell(colIndex).toString() + ";"
//														: " ";
//											}
//
//											try {
//												row.getCell(colIndex + 1).getCellFormula();
//												quantity += row.getCell(colIndex + 1) != null
//														? row.getCell(colIndex + 1).getRawValue().toString() + ";"
//														: " ";
//
//											} catch (Exception e) {
//												quantity += row.getCell(colIndex + 1) != null
//														? row.getCell(colIndex + 1).toString() + ";"
//														: " ";
//											}
//											gridCols.get(0).put("uda_value", type);
//											gridCols.get(1).put("uda_value", quantity);
//											colIndex += 2;
//											if (gridCols.get(0).get("uda_value").toString().endsWith(";;")) {
//												gridCols.get(0).put("uda_value",
//														gridCols.get(0).get("uda_value").toString().replace(";;", ""));
//												gridCols.get(1).put("uda_value",
//														gridCols.get(1).get("uda_value").toString().replace(";;", ""));
//												return gridCols;
//											}
//										}
//
//										// String x =
//										// gridCols.get(0).get("uda_value").toString().substring(gridCols.get(0).get("uda_value").toString().length()
//										// - 1,
//										// gridCols.get(0).get("uda_value").toString().length() - 2);
//										return gridCols;
//									} else if (uda.get("name").toString().contains("Ladder information")) {// static
//																											// handling
//																											// for
//										// Ladder information grid
//										int gridColIndex = 0;
//										while (gridColIndex < gridCols.size()) {
//											row = workSheet.getRow(i);
//											if (cellNotEmpty(row.getCell(1))) {
//												try {
//													row.getCell(1).getCellFormula();
//													columnValue = row.getCell(colIndex) != null
//															? row.getCell(1).getRawValue().toString()
//															: " ";
//
//												} catch (Exception e) {
//													columnValue = row.getCell(1) != null
//															? row.getCell(1).toString().trim().replace(":", "")
//															: " ";
//												}
//												if (columnValue.toLowerCase().matches(
//														gridCols.get(gridColIndex).get("name").toString().toLowerCase())
//														&& row.getPhysicalNumberOfCells() >= 2) {// col is found
//													gridCols.get(gridColIndex).put("uda_value",
//															row.getCell(2).toString());
//												}
//											}
//											gridColIndex++;
//											i++; // go to next row to get col value
//
//										}
//									} else {
//
//										int gridColIndex = 0;
//										while (gridColIndex < gridCols.size()) {
//											i++; // go to next row to get col value
//											row = workSheet.getRow(i);
//											if (cellNotEmpty(row.getCell(colIndex))) {
//												try {
//													row.getCell(colIndex).getCellFormula();
//													columnValue = row.getCell(colIndex) != null
//															? row.getCell(colIndex).getRawValue().toString()
//															: " ";
//
//												} catch (Exception e) {
//													columnValue = row.getCell(colIndex) != null
//															? row.getCell(colIndex).toString().trim().replace(":", "")
//															: " ";
//												}
//												if (columnValue.toLowerCase().matches(
//														gridCols.get(gridColIndex).get("name").toString().toLowerCase())
//														&& row.getPhysicalNumberOfCells() >= colIndex + 1) {// col is
//																											// found
//													gridCols.get(gridColIndex).put("uda_value",
//															row.getCell(colIndex + 1).toString());
//												}
//											}
//											gridColIndex++;
//										}
//										return gridCols;
//									}
//								}
//							}
//						}
//					}
//				}
//				sheetIndex++;
//
//			}
//			return gridCols;
//
//		}
//
//		private boolean cellNotEmpty(XSSFCell cell) {
//			return cell != null && cell.toString() != "";
//		}
//
//		private String getUDAValueFromTemplate(JSONObject uda, String templateUrl)
//				throws InvalidFormatException, IOException {
//			XSSFWorkbook workBook = new XSSFWorkbook(new File(templateUrl));
//			String udaNameInSheet = uda.get("name_in_excel") != null ? uda.get("name_in_excel").toString()
//					: uda.get("name").toString();
//
//			int sheetIndex = 1;
//			while (sheetIndex <= 2) {
//				XSSFSheet workSheet = workBook.getSheetAt(sheetIndex);
//
//				for (int i = 0; i < workSheet.getPhysicalNumberOfRows(); i++) {
//					XSSFRow row = workSheet.getRow(i);
//					if (row != null && cellNotEmpty(row.getCell(0))) {
//
//						if (row != null) {
//							for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
////								System.out.println("row:" + i + " col:" + colIndex);
//								if (cellNotEmpty(row.getCell(colIndex))) {
//									String columnValue;
//									try {
//										row.getCell(colIndex).getCellFormula();
//										columnValue = row.getCell(colIndex) != null
//												? row.getCell(colIndex).getRawValue().toString()
//												: " ";
//
//									} catch (Exception e) {
//										columnValue = row.getCell(colIndex) != null ? row.getCell(colIndex).toString()
//												: " ";
//									}
//									if (columnValue.contains(udaNameInSheet)) {
////										System.out.println("found UDA: " + uda.get("name") + "udaValue: ");
//										return colIndex + 1 <= row.getPhysicalNumberOfCells()
//												? row.getCell(colIndex + 1).toString()
//												: "";
//									}
//								}
//							}
//						}
//					}
//				}
//				sheetIndex++;
//			}
//			return null;
//		}
//
//		private boolean isGridCol(XSSFCell DBTableCell) {
//			return DBTableCell == null;
//		}
//
//		private JSONObject getGridCols(XSSFSheet uDASWorkSheet, int rowIndex, XSSFRow header, String templateUrl)
//				throws InvalidFormatException, IOException {
//			JSONObject obj;
//			List<JSONObject> gridCols = new ArrayList<JSONObject>();
//			while (rowIndex < uDASWorkSheet.getPhysicalNumberOfRows()) {
//				XSSFRow row = uDASWorkSheet.getRow(rowIndex);
//				if (row != null && cellNotEmpty(row.getCell(0))) {
//					JSONObject rowJsonObject = new JSONObject();
//					if (isGridCol(row.getCell(3))) {
//						for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
////							System.out.println("row:" + rowIndex + "  col:" + colIndex);
//							if (cellNotEmpty(row.getCell(colIndex))) {
//								rowJsonObject.put(header.getCell(colIndex).toString().toLowerCase().replace(" ", "_"),
//										row.getCell(colIndex).toString());
//							}
//							// grid has no rule in temp1 so it can not be dynamic
//
//						}
//					} else {
//						obj = new JSONObject();
//						obj.put("lastIndex", rowIndex);
//						obj.put("gridCols", gridCols);
//						return obj;
//					}
//					if (!rowJsonObject.isEmpty()) {
//						gridCols.add(rowJsonObject);
//					}
//					rowIndex++;
//				}
//			}
//			obj = new JSONObject();
//			obj.put("lastIndex", rowIndex);
//			obj.put("gridCols", gridCols);
//			return obj;
//		}
//
//	}
//}
