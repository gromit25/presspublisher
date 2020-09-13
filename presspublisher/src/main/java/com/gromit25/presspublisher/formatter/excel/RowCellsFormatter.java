package com.gromit25.presspublisher.formatter.excel;

import java.nio.charset.Charset;

import com.gromit25.presspublisher.evaluator.ValueContainer;
import com.gromit25.presspublisher.formatter.FormatterException;
import com.gromit25.presspublisher.formatter.FormatterSpec;

/**
 * 내부의 cell들의 커서이동을 오른쪽으로 이동하는 formatter 
 * 
 * @author jmsohn
 */
@FormatterSpec(group="excel", tag="rowcells")
public class RowCellsFormatter extends AbstractExcelFormatter {

	@Override
	protected void formatExcel(WorksheetFormatter copy, Charset charset, ValueContainer values) throws FormatterException {
		
		// 1. Worksheet 하위 컴포넌트 수행 전 작업
		//    - Worksheet의 Cell 수행 완료 후 자동 이동 방향을 오른쪽(RIGHT)로 변경
		//    - Worksheet의 하위 컴포넌트 수행 후 원래의 컬럼 위치로 옮기기 위해,
		//      현재 Worksheet의 커서의 컬럼값 저장
		copy.setCursorDirection(WorksheetFormatter.CursorDirection.RIGHT);
		int savedColumnPosition = copy.getCursorColumnPosition();
		
		// 2. Worksheet의 하위 컴포넌트를 수행함
		this.execChildFormatters(copy, charset, values);
		
		// 3. Worksheet 하위 컴포넌트 수행 후 작업
		//    - Worksheet의 Cell 자동이동 방향을 원래 방향(DOWN)으로 변경
		//    - Row를 한칸 아래로 설정
		//    - Worksheet의 커서 컬럼을 원래대로 복구함
		copy.setCursorDirection(WorksheetFormatter.CursorDirection.DOWN);
		
		copy.setCursorRowPosition(copy.getCursorRowPosition() + 1);
		copy.setCursorColumnPosition(savedColumnPosition);
		
	}

}