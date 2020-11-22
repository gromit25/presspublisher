package com.gromit25.presspublisher.formatter.excel;

import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.gromit25.presspublisher.evaluator.ValueContainer;
import com.gromit25.presspublisher.formatter.FormatterAttr;
import com.gromit25.presspublisher.formatter.FormatterException;
import com.gromit25.presspublisher.formatter.FormatterSpec;

import lombok.Getter;
import lombok.Setter;

/**
 * row 삽입
 * @author jmsohn
 */
@FormatterSpec(group="excel", tag="insert-row")
public class InsertRow extends AbstractExcelFormatter {
	
	/**
	 * row를 삽입(insert)할 위치
	 */
	@Getter
	@Setter
	@FormatterAttr(name="at", mandatory=false)
	private int at = -1;

	@Override
	protected void formatExcel(OutputStream out, Charset charset, ValueContainer values) throws FormatterException {
		
		WorksheetFormatter sheetFormatter = this.getParentInBranch(WorksheetFormatter.class);
		
		// row를 삽입할 위치를 가져옴
		// xml에 위치(at)가 설정되어 있지 않은 경우(at == -1)
		// 현재 커서의 row 위치를 기준으로 함
		int at = this.getAt();
		if(-1 == at) {
			at = sheetFormatter.getCursorRowPosition();
		}
		
		// 현재 row를 한칸 아래로 shift함
		XSSFSheet sheet = sheetFormatter.getWorksheet();
		sheet.shiftRows(at, at, 1);
		
		// 상단 row의 style을 복사함
		// 만일 삽입할 위치가 최상단일 경우 복사하지 않음
		if(0 < at) {
			CellCopyPolicy policy = new CellCopyPolicy.Builder()
									.cellStyle(true)
									.build();
			sheet.copyRows(at-1, at-1, at, policy);
		}
		
		// rowcells의 하위 컴포넌트를 수행함
		this.execChildFormatters(out, charset, values);
	}

}