import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ArtemisTestModule } from '../../../../test.module';
import { MockTranslateService } from '../../../../helpers/mocks/service/mock-translate.service';
import { TranslateService } from '@ngx-translate/core';
import { MockPipe } from 'ng-mocks';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { ArtemisDatePipe } from 'app/shared/pipes/artemis-date.pipe';
import { AverageActionsChartComponent } from 'app/exam/monitoring/charts/activity-log/average-actions-chart.component';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ChartTitleComponent } from 'app/exam/monitoring/charts/chart-title.component';
import dayjs from 'dayjs/esm';
import { TotalActionsChartComponent } from 'app/exam/monitoring/charts/activity-log/total-actions-chart.component';
import { ActionsChartComponent } from 'app/exam/monitoring/charts/activity-log/actions-chart.component';
import { createActions, createSingleSeriesDataEntriesWithTimestamps } from '../exam-monitoring-helper';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { MockWebsocketService } from '../../../../helpers/mocks/service/mock-websocket.service';
import { Course } from 'app/entities/course.model';
import { Exam } from 'app/entities/exam.model';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { ceilDayjsSeconds } from 'app/exam/monitoring/charts/monitoring-chart';
import { ExamAction } from 'app/entities/exam-user-activity.model';

describe('Total Actions Chart Component', () => {
    let comp: TotalActionsChartComponent;
    let fixture: ComponentFixture<TotalActionsChartComponent>;
    let pipe: ArtemisDatePipe;

    // Course
    const course = new Course();
    course.id = 1;

    // Exam
    const exam = new Exam();
    exam.id = 1;

    const now = dayjs();
    let ceiledNow: dayjs.Dayjs;

    const route = { parent: { params: of({ courseId: course.id, examId: exam.id }) } };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, NgxChartsModule],
            declarations: [AverageActionsChartComponent, ChartTitleComponent, ActionsChartComponent, ArtemisDatePipe, MockPipe(ArtemisTranslatePipe)],
            providers: [
                { provide: ActivatedRoute, useValue: route },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: ArtemisDatePipe },
                { provide: JhiWebsocketService, useClass: MockWebsocketService },
            ],
        })
            .compileComponents()
            .then(() => {
                pipe = new ArtemisDatePipe(TestBed.inject(TranslateService));
                fixture = TestBed.createComponent(TotalActionsChartComponent);
                comp = fixture.componentInstance;
                ceiledNow = ceilDayjsSeconds(now, comp.timeStampGapInSeconds);
            });
    });

    afterEach(() => {
        // completely restore all fakes created through the sandbox
        jest.restoreAllMocks();
    });

    // On init
    it('should call initData on init without actions', () => {
        expect(comp.ngxData).toEqual([]);

        const series = createSingleSeriesDataEntriesWithTimestamps(comp.getLastXTimestamps(), pipe);

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.ngxData).toEqual([{ name: 'actions', series }]);
    });

    it('should call initData on init with actions', () => {
        // Create series
        const series = createSingleSeriesDataEntriesWithTimestamps(comp.getLastXTimestamps(), pipe);

        // GIVEN
        comp.filteredExamActions = createActions().map((action) => {
            action.timestamp = now;
            action.ceiledTimestamp = ceiledNow;
            return action;
        });

        // Insert value
        series.filter((data) => data.name === pipe.transform(ceiledNow, 'time', true).toString())[0].value = comp.filteredExamActions.length;

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.ngxData).toEqual([{ name: 'actions', series }]);
    });

    // Evaluate and add action
    it.each(createActions())('should evaluate and add action', (action: ExamAction) => {
        expect(comp.filteredExamActions).toEqual([]);

        action.ceiledTimestamp = ceiledNow;

        comp.evaluateAndAddAction(action);

        const expectedMap = new Map();
        expectedMap.set(ceiledNow.toString(), 1);

        expect(comp.filteredExamActions).toEqual([action]);
        expect(comp.actionsPerTimestamp).toEqual(expectedMap);
    });
});
