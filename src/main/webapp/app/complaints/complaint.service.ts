import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import dayjs from 'dayjs/esm';

import { Complaint, ComplaintType } from 'app/entities/complaint.model';
import { ComplaintResponseService } from 'app/complaints/complaint-response.service';
import { Exercise } from 'app/entities/exercise.model';
import { map } from 'rxjs/operators';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { Course } from 'app/entities/course.model';

export type EntityResponseType = HttpResponse<Complaint>;
export type EntityResponseTypeArray = HttpResponse<Complaint[]>;

export interface IComplaintService {
    isComplaintLockedForLoggedInUser: (complaint: Complaint, exercise: Exercise) => boolean | undefined;
    isComplaintLockedByLoggedInUser: (complaint: Complaint) => boolean | undefined;
    isComplaintLocked: (complaint: Complaint) => boolean | undefined;
    create: (complaint: Complaint, examId: number) => Observable<EntityResponseType>;
    findBySubmissionId: (participationId: number) => Observable<EntityResponseType>;
    getComplaintsForTestRun: (exerciseId: number) => Observable<EntityResponseTypeArray>;
    getMoreFeedbackRequestsForTutor: (exerciseId: number) => Observable<EntityResponseTypeArray>;
    getNumberOfAllowedComplaintsInCourse: (courseId: number) => Observable<number>;
    findAllByTutorIdForCourseId: (tutorId: number, courseId: number, complaintType: ComplaintType) => Observable<EntityResponseTypeArray>;
    findAllByTutorIdForExerciseId: (tutorId: number, exerciseId: number, complaintType: ComplaintType) => Observable<EntityResponseTypeArray>;
    findAllByCourseId: (courseId: number, complaintType: ComplaintType) => Observable<EntityResponseTypeArray>;
    findAllByCourseIdAndExamId: (courseId: number, examId: number) => Observable<EntityResponseTypeArray>;
    findAllByExerciseId: (exerciseId: number, complaintType: ComplaintType) => Observable<EntityResponseTypeArray>;
}

@Injectable({ providedIn: 'root' })
export class ComplaintService implements IComplaintService {
    private apiUrl = SERVER_API_URL + 'api';
    private resourceUrl = this.apiUrl + '/complaints';

    constructor(private http: HttpClient, private complaintResponseService: ComplaintResponseService) {}

    /**
     * Checks if a complaint is locked for the currently logged-in user
     *
     * A complaint is locked if the associated complaint response is locked
     *
     * @param complaint complaint to check the lock status for
     * @param exercise exercise used to find out if currently logged-in user is instructor
     */
    isComplaintLockedForLoggedInUser(complaint: Complaint, exercise: Exercise) {
        if (complaint.complaintResponse && complaint.accepted === undefined) {
            return this.complaintResponseService.isComplaintResponseLockedForLoggedInUser(complaint.complaintResponse, exercise);
        } else {
            return false;
        }
    }

    /**
     * Checks if the lock on a complaint is active and if the currently logged-in user is the creator of the lock
     * @param complaint complaint to check the lock status for
     */
    isComplaintLockedByLoggedInUser(complaint: Complaint) {
        if (complaint.complaintResponse && complaint.accepted === undefined) {
            return this.complaintResponseService.isComplaintResponseLockedByLoggedInUser(complaint.complaintResponse);
        } else {
            return false;
        }
    }

    /**
     * Checks if a complaint is locked
     * @param complaint complaint to check lock status for
     */
    isComplaintLocked(complaint: Complaint) {
        if (complaint.complaintResponse && complaint.accepted === undefined) {
            return complaint.complaintResponse.isCurrentlyLocked;
        } else {
            return false;
        }
    }

    /**
     * Create a new complaint.
     * @param complaint
     * @param examId the id of the exam
     */
    create(complaint: Complaint, examId?: number): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(complaint);
        if (examId) {
            return this.http
                .post<Complaint>(`${this.resourceUrl}/exam/${examId}`, copy, { observe: 'response' })
                .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
        }
        return this.http.post<Complaint>(this.resourceUrl, copy, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    /**
     * Find complaint by Submission id.
     * @param submissionId
     */
    findBySubmissionId(submissionId: number): Observable<EntityResponseType> {
        return this.http
            .get<Complaint>(`${this.resourceUrl}/submissions/${submissionId}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    /**
     * Find complaints for instructor for specified test run exercise (complaintType == 'COMPLAINT').
     * @param exerciseId
     */
    getComplaintsForTestRun(exerciseId: number): Observable<EntityResponseTypeArray> {
        return this.http
            .get<Complaint[]>(`${this.apiUrl}/exercises/${exerciseId}/complaints-for-test-run-dashboard`, { observe: 'response' })
            .pipe(map((res: EntityResponseTypeArray) => this.convertDateFromServerArray(res)));
    }

    /**
     * Find more feedback requests for tutor in this exercise.
     * @param exerciseId
     */
    getMoreFeedbackRequestsForTutor(exerciseId: number): Observable<EntityResponseTypeArray> {
        return this.http
            .get<Complaint[]>(`${this.apiUrl}/exercises/${exerciseId}/more-feedback-for-assessment-dashboard`, { observe: 'response' })
            .pipe(map((res: EntityResponseTypeArray) => this.convertDateFromServerArray(res)));
    }

    /**
     * Get number of allowed complaints in this course.
     * @param courseId
     * @param teamMode If true, the number of allowed complaints for the user's team is returned
     */
    getNumberOfAllowedComplaintsInCourse(courseId: number, teamMode = false): Observable<number> {
        return this.http.get<number>(`${this.apiUrl}/courses/${courseId}/allowed-complaints?teamMode=${teamMode}`);
    }

    /**
     * Find all complaints by tutor id, course id and complaintType.
     * @param tutorId
     * @param courseId
     * @param complaintType
     */
    findAllByTutorIdForCourseId(tutorId: number, courseId: number, complaintType: ComplaintType): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/courses/${courseId}/complaints?complaintType=${complaintType}&tutorId=${tutorId}`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Find all complaints by course id and complaintType without student's information
     * @param courseId - the course id for which the complaints should be retrieved
     * @param complaintType - the type of complaint
     */
    findAllWithoutStudentInformationForCourseId(courseId: number, complaintType: ComplaintType): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/courses/${courseId}/complaints?complaintType=${complaintType}&allComplaintsForTutor=true`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Find all complaints by tutor id, exercise id and complaintType.
     * @param tutorId
     * @param exerciseId
     * @param complaintType
     */
    findAllByTutorIdForExerciseId(tutorId: number, exerciseId: number, complaintType: ComplaintType): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/exercises/${exerciseId}/complaints?complaintType=${complaintType}&tutorId=${tutorId}`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Find all complaints by course id and complaintType.
     * @param courseId
     * @param complaintType
     */
    findAllByCourseId(courseId: number, complaintType: ComplaintType): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/courses/${courseId}/complaints?complaintType=${complaintType}`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Find all complaints by course id and exam id.
     * @param courseId
     * @param examId
     */
    findAllByCourseIdAndExamId(courseId: number, examId: number): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/courses/${courseId}/exams/${examId}/complaints`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Find all complaints by exercise id and complaintType.
     * @param exerciseId
     * @param complaintType
     */
    findAllByExerciseId(exerciseId: number, complaintType: ComplaintType): Observable<EntityResponseTypeArray> {
        const url = `${this.apiUrl}/exercises/${exerciseId}/complaints?complaintType=${complaintType}`;
        return this.requestComplaintsFromUrl(url);
    }

    /**
     * Returns the time needed to evaluate the complaint. If it hasn't been evaluated yet, the difference between the submission time and now is used.
     * @param complaint for which the response time should be calculated
     * @return returns the passed time in seconds
     */
    getResponseTimeInSeconds(complaint: Complaint): number {
        if (complaint.accepted !== undefined) {
            return complaint.complaintResponse?.submittedTime?.diff(complaint.submittedTime, 'seconds') || NaN;
        } else {
            return dayjs().diff(complaint.submittedTime, 'seconds');
        }
    }

    /**
     * Determines if the complaint should be highlighted. This is the case if the complaint hasn't been reviewed and was submitted more than one week ago.
     * @param complaint for which it should be determined if highlighting is needed
     * @return returns true iff the complaint should be highlighted
     */
    shouldHighlightComplaint(complaint: Complaint): boolean {
        if (complaint.accepted !== undefined) {
            return false;
        }

        const complaintSubmittedTime = complaint.submittedTime;
        if (complaintSubmittedTime) {
            return dayjs().diff(complaintSubmittedTime, 'days') > 7;
        }

        return false;
    }

    /**
     * Calculates the date until which a complaint can be filed at least
     * @param exercise for which the student can complain
     * @param studentParticipation of the student in the exercise that can complain
     * @param course in which the exercise is
     * @return the date until which the student can complain
     */
    getIndividualComplaintDueDate(exercise: Exercise, course: Course, studentParticipation?: StudentParticipation): dayjs.Dayjs | undefined {
        const lastResult = studentParticipation?.results?.last();
        const now = dayjs();
        if (!lastResult) {
            return undefined;
        }

        let complaintStartDate;
        if (exercise.allowComplaintsForAutomaticAssessments && now.isAfter(exercise.dueDate)) {
            complaintStartDate = exercise.dueDate;
        } else if (lastResult.rated && !exercise.assessmentDueDate) {
            complaintStartDate = lastResult.completionDate;
        } else if (lastResult.rated && now.isAfter(exercise.assessmentDueDate)) {
            complaintStartDate = exercise.assessmentDueDate!.isAfter(lastResult.completionDate) ? exercise.assessmentDueDate : lastResult.completionDate;
        } else {
            return undefined;
        }

        return dayjs(complaintStartDate).add(course.maxComplaintTimeDays!, 'days');
    }

    private requestComplaintsFromUrl(url: string): Observable<EntityResponseTypeArray> {
        return this.http.get<Complaint[]>(url, { observe: 'response' }).pipe(map((res: EntityResponseTypeArray) => this.convertDateFromServerArray(res)));
    }

    private convertDateFromClient(complaint: Complaint): Complaint {
        return Object.assign({}, complaint, {
            submittedTime: complaint.submittedTime && dayjs(complaint.submittedTime).isValid ? complaint.submittedTime.toJSON() : undefined,
        });
    }

    private convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.submittedTime = res.body.submittedTime ? dayjs(res.body.submittedTime) : undefined;
            if (res.body?.complaintResponse) {
                this.complaintResponseService.convertDatesToDayjs(res.body.complaintResponse);
            }
        }
        return res;
    }

    private convertDateFromServerArray(res: EntityResponseTypeArray): EntityResponseTypeArray {
        if (res.body) {
            res.body.forEach((complaint) => {
                complaint.submittedTime = complaint.submittedTime ? dayjs(complaint.submittedTime) : undefined;
                if (complaint.complaintResponse) {
                    this.complaintResponseService.convertDatesToDayjs(complaint.complaintResponse);
                }
            });
        }
        return res;
    }
}
